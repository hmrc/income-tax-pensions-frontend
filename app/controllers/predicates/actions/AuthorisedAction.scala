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
import models.error.MissingAgentClientDetails
import models.logging.CorrelationIdMdc.withEnrichedCorrelationId
import models.{AuthorisationRequest, User}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{AuthService, SessionDataService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{EnrolmentHelper, SessionHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(val appConfig: AppConfig,
                                 errorHandler: ErrorHandler,
                                 authService: AuthService,
                                 sessionDataService: SessionDataService
                                )(implicit mcc: MessagesControllerComponents)
  extends ActionBuilder[AuthorisationRequest, AnyContent] with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  implicit val config: AppConfig                  = appConfig
  implicit val messagesApi: MessagesApi           = mcc.messagesApi

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  override def invokeBlock[A](original: Request[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] = {
    withEnrichedCorrelationId(original) { request =>

      implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      implicit val req: Request[A] = request

      withSessionId { sessionId =>
        authService.authorised().retrieve(affinityGroup) {
          case Some(AffinityGroup.Agent) => agentAuthentication(block, sessionId)(request, headerCarrier)
          case Some(affinityGroup) => individualAuthentication(block, affinityGroup, sessionId)(request, headerCarrier)
          case None => throw new UnauthorizedException("Unable to retrieve affinityGroup")
        } recover {
          case _: NoActiveSession =>
            Redirect(appConfig.signInUrl)
          case _: AuthorisationException =>
            logger.warn(s"[AuthorisedAction][invokeBlock] - User failed to authenticate")
            Redirect(controllers.errors.routes.UnauthorisedUserErrorController.show)
          case e =>
            logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            errorHandler.internalServerError()(request)
        }
      }
    }
  }

  def individualAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                  affinityGroup: AffinityGroup,
                                  sessionId: String
                                 )(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    authService.authorised().retrieve(allEnrolments and confidenceLevel) {
      case enrolments ~ userConfidence if userConfidence.level >= ConfidenceLevel.L250.level =>
        (
          EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments),
          EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)
        ) match {
          case (Some(mtdItId), Some(nino)) =>
            block(AuthorisationRequest(User(mtdItId, None, nino, sessionId, affinityGroup.toString), request))
          case (_, None) =>
            Future.successful(Redirect(appConfig.signInUrl))
          case (None, _) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment. Redirecting user to sign up for MTD.")
            Future.successful(Redirect(controllers.errors.routes.IndividualAuthErrorController.show))
        }
      case _ =>
        logger.info("[AuthorisedAction][individualAuthentication] User has confidence level below 250, routing user to IV uplift.")
        Future(Redirect(appConfig.incomeTaxSubmissionIvRedirect))
    }

  private[predicates] def agentAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                                 sessionId: String
                                                )(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    sessionDataService.getSessionData(sessionId).flatMap { sessionData =>
      authService
        .authorised(EnrolmentHelper.agentAuthPredicate(sessionData.mtditid))
        .retrieve(allEnrolments) {
          populateAgent(block, sessionData.mtditid, sessionData.nino, sessionId, _, AffinityGroup.Agent, isSecondaryAgent = false)
        }
        .recoverWith(agentRecovery(block, sessionData.mtditid, sessionData.nino, sessionId, AffinityGroup.Agent))
    }.recover {
      case _: MissingAgentClientDetails =>
        Redirect(appConfig.viewAndChangeEnterUtrUrl)
    }

  private def agentRecovery[A](block: AuthorisationRequest[A] => Future[Result],
                               mtdItId: String,
                               nino: String,
                               sessionId: String,
                               affinityGroup: AffinityGroup
                              )(implicit request: Request[A], hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession => Future(Redirect(appConfig.signInUrl))
    case _: AuthorisationException =>
      authService
        .authorised(EnrolmentHelper.secondaryAgentPredicate(mtdItId))
        .retrieve(allEnrolments) {
          populateAgent(block, mtdItId, nino, sessionId, _, affinityGroup, isSecondaryAgent = true)
        }
        .recoverWith {
          case _: AuthorisationException =>
            logger.info(s"[AuthorisedAction][agentAuthentication] - Agent does not have secondary agent delegated authority for Client.")
            Future(Redirect(controllers.errors.routes.AgentAuthErrorController.show))
          case e =>
            logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            errorHandler.futureInternalServerError()
        }
    case e =>
      logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      errorHandler.futureInternalServerError()
  }

  private def populateAgent[A](block: AuthorisationRequest[A] => Future[Result],
                               mtdItId: String,
                               nino: String,
                               sessionId: String,
                               enrolments: Enrolments,
                               affinityGroup: AffinityGroup,
                               isSecondaryAgent: Boolean)(implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    if (isSecondaryAgent) {
      logger.warn(s"[AuthorisedAction][agentAuthentication] - Secondary agent unauthorised")
      Future.successful(Redirect(controllers.errors.routes.SupportingAgentAuthErrorController.show))
    } else {
      EnrolmentHelper.getEnrolmentValueOpt(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
        case Some(arn) =>
          block(AuthorisationRequest(User(mtdItId, Some(arn), nino, sessionId, affinityGroup.toString, isSecondaryAgent), request))
        case None =>
          logger.warn(s"[AuthorisedAction][agentAuthentication] - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
          Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show))
      }
    }
}
