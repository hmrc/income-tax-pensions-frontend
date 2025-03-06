/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.predicates.actions

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.{AppConfig, ErrorHandler}
import models.logging.CorrelationIdMdc.withEnrichedCorrelationId
import models.{AuthorisationRequest, User}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject() (appConfig: AppConfig, errorHandler: ErrorHandler)(implicit
    val authService: AuthService,
    val mcc: MessagesControllerComponents)
    extends ActionBuilder[AuthorisationRequest, AnyContent]
    with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger                         = Logger.apply(this.getClass)
  implicit val config: AppConfig                  = appConfig
  implicit val messagesApi: MessagesApi           = mcc.messagesApi

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  val minimumConfidenceLevel: Int = ConfidenceLevel.L250.level

  private def sessionIdBlock(errorLogString: String, errorAction: Future[Result])(
      block: String => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] =
    hc.sessionId match {
      case Some(sessionId) => block(sessionId.value)
      case _ =>
        request.headers.get(SessionKeys.sessionId) match {
          case Some(sessionId) => block(sessionId)
          case _ =>
            logger.error(errorLogString)
            errorAction
        }
    }

  override def invokeBlock[A](original: Request[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] =
    withEnrichedCorrelationId(original) { request =>
      implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      authService.authorised().retrieve(affinityGroup) {
        case Some(AffinityGroup.Agent) => agentAuthentication(block)(request, headerCarrier)
        case Some(affinityGroup)       => individualAuthentication(block, affinityGroup)(request, headerCarrier)
        case None                      => throw new UnauthorizedException("Unable to retrieve affinityGroup")
      } recover {
        case _: NoActiveSession =>
          logger.warn(s"[AuthorisedAction][invokeBlock] - No active session. Redirecting to ${appConfig.signInUrl}")
          Redirect(appConfig.signInUrl)
        case _: AuthorisationException =>
          logger.warn(s"[AuthorisedAction][invokeBlock] - User failed to authenticate")
          Redirect(controllers.errors.routes.UnauthorisedUserErrorController.show)
        case e =>
          logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
          errorHandler.internalServerError()(request)
      }
    }

  def individualAuthentication[A](block: AuthorisationRequest[A] => Future[Result], affinityGroup: AffinityGroup)(implicit
      request: Request[A],
      hc: HeaderCarrier): Future[Result] =
    authService.authorised().retrieve(allEnrolments and confidenceLevel) {

      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        val optionalMtdItId: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments)
        val optionalNino: Option[String]    = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

        (optionalMtdItId, optionalNino) match {
          case (Some(mtdItId), Some(nino)) =>
            sessionIdBlock(
              errorLogString = s"[AuthorisedAction][individualAuthentication] - No session id in request",
              errorAction = Future.successful(Redirect(appConfig.signInUrl))
            )(sessionId => block(AuthorisationRequest(User(mtdItId, None, nino, sessionId, affinityGroup.toString), request)))

          case (_, None) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - No active session. Redirecting to ${appConfig.signInUrl}")
            Future.successful(Redirect(appConfig.signInUrl))
          case (None, _) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment. Redirecting user to sign up for MTD.")
            Future.successful(Redirect(controllers.errors.routes.IndividualAuthErrorController.show))
        }
      case _ =>
        logger.warn("[AuthorisedAction][individualAuthentication] User has confidence level below 250, routing user to IV uplift.")
        Future(Redirect(appConfig.incomeTaxSubmissionIvRedirect))
    }

  private[predicates] def agentAuthPredicate(mtdId: String): Predicate =
    Enrolment(EnrolmentKeys.Individual)
      .withIdentifier(EnrolmentIdentifiers.individualId, mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.agentDelegatedAuthRule)

  private[predicates] def secondaryAgentPredicate(mtdId: String): Predicate =
    Enrolment(EnrolmentKeys.SupportingAgent)
      .withIdentifier(EnrolmentIdentifiers.individualId, mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.supportingAgentDelegatedAuthRule)

  private val agentAuthLogString: String = "[AuthorisedAction][agentAuthentication]"

  private[predicates] def agentAuthentication[A](
      block: AuthorisationRequest[A] => Future[Result])(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    (request.session.get(SessionValues.CLIENT_MTDITID), request.session.get(SessionValues.CLIENT_NINO)) match {
      case (Some(mtdItId), Some(nino)) =>
        authService
          .authorised(agentAuthPredicate(mtdItId))
          .retrieve(allEnrolments) {
            populateAgent(block, mtdItId, nino, _, AffinityGroup.Agent, isSecondaryAgent = false)
          }
          .recoverWith(agentRecovery(block, mtdItId, nino, AffinityGroup.Agent))

      case (mtditid, nino) =>
        logger.warn(
          s"$agentAuthLogString - Agent does not session key values. Redirecting to view & change." +
            s"MTDITID missing:${mtditid.isEmpty}, NINO missing:${nino.isEmpty}")
        Future.successful(Redirect(appConfig.viewAndChangeEnterUtrUrl))
    }

  private def agentRecovery[A](block: AuthorisationRequest[A] => Future[Result], mtdItId: String, nino: String, affinityGroup: AffinityGroup)(implicit
      request: Request[A],
      hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      logger.warn(s"$agentAuthLogString - No active session. Redirecting to ${appConfig.signInUrl}")
      Future(Redirect(appConfig.signInUrl))
    case _: AuthorisationException =>
      authService
        .authorised(secondaryAgentPredicate(mtdItId))
        .retrieve(allEnrolments) {
          populateAgent(block, mtdItId, nino, _, affinityGroup, isSecondaryAgent = true)
        }
        .recoverWith {
          case _: AuthorisationException =>
            logger.warn(s"$agentAuthLogString - Agent does not have secondary delegated authority for Client.")
            Future(Redirect(controllers.errors.routes.AgentAuthErrorController.show))
          case e =>
            logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            errorHandler.futureInternalServerError()
        }
    case e =>
      logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      errorHandler.futureInternalServerError()
  }

  private def populateAgent[A](block: AuthorisationRequest[A] => Future[Result],
                               mtdItId: String,
                               nino: String,
                               enrolments: Enrolments,
                               affinityGroup: AffinityGroup,
                               isSecondaryAgent: Boolean)(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    isSecondaryAgent match {
      case true =>
        logger.warn(s"$agentAuthLogString - Secondary agent unauthorised")
        Future.successful(Redirect(controllers.errors.routes.SupportingAgentAuthErrorController.show))
      case false =>
        enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
          case Some(arn) =>
            sessionIdBlock(
              errorLogString = s"$agentAuthLogString - No session id in request",
              errorAction = Future.successful(Redirect(appConfig.signInUrl))
            )(sessionId => block(AuthorisationRequest(User(mtdItId, Some(arn), nino, sessionId, affinityGroup.toString, isSecondaryAgent), request)))
          case None =>
            logger.warn(s"$agentAuthLogString - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
            Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show))
        }
    }

  private[predicates] def enrolmentGetIdentifierValue(
      checkedKey: String,
      checkedIdentifier: String,
      enrolments: Enrolments
  ): Option[String] = enrolments.enrolments.collectFirst { case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) =>
    enrolmentIdentifiers.collectFirst { case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) =>
      identifierValue
    }
  }.flatten

}
