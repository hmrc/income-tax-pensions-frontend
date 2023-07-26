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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.requests.UserPriorAndSessionDataRequest
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.CYAPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import services.{ExcludeJourneyService, PensionChargesService, PensionSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsCYAController @Inject()(auditProvider: AuditActionsProvider,
                                                  view: UnauthorisedPaymentsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  pensionChargesService: PensionChargesService,
                                                  errorHandler: ErrorHandler,
                                                  excludeJourneyService: ExcludeJourneyService)
                                                 (implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig, clock: Clock,
                                                  ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = auditProvider.unauthorisedPaymentsViewAuditing(taxYear) async {
    implicit sessionDataRequest =>

      val cyaData = sessionDataRequest.pensionsUserData
      if (!cyaData.pensions.unauthorisedPayments.isFinished) {
        val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) { data =>
          Future.successful(Ok(view(taxYear, data.pensions.unauthorisedPayments)))
        }
      } else {
        pensionSessionService.createOrUpdateSessionData(sessionDataRequest.user, cyaData.pensions, taxYear, isPriorSubmission = false)(
          errorHandler.internalServerError())(Ok(view(taxYear, cyaData.pensions.unauthorisedPayments)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.unauthorisedPaymentsUpdateAuditing(taxYear) async {
    implicit priorAndSessionRequest =>

      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(priorAndSessionRequest.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>
          val (cya, prior, unauthP) = (data, priorAndSessionRequest.pensions, data.pensions.unauthorisedPayments)

          if (!unauthP.surchargeQuestion.exists(x => x) && !unauthP.noSurchargeQuestion.exists(x => x)) {
            //TODO: check conditions for excluding Pensions from submission without gateway
            excludeJourneyService.excludeJourney("pensions", taxYear, priorAndSessionRequest.user.nino)(priorAndSessionRequest.user, hc)
          } flatMap {
            case Right(_) => performSubmission(taxYear, Some(cya))(priorAndSessionRequest.user, hc, priorAndSessionRequest, clock)
            case Left(_) => errorHandler.futureInternalServerError()
          }
          if (!comparePriorData(cya.pensions, prior)) Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          else performSubmission(taxYear, Some(cya))(priorAndSessionRequest.user, hc, priorAndSessionRequest, clock)
      }
  }

  private def performSubmission(taxYear: Int, cya: Option[PensionsUserData])
                               (implicit user: User,
                                hc: HeaderCarrier,
                                request: UserPriorAndSessionDataRequest[AnyContent],
                                clock: Clock): Future[Result] = {
    (cya match {
      case Some(_) =>
        pensionChargesService.saveUnauthorisedViewModel(user, taxYear) map {
          case Left(_) =>
            logger.info("[submit] Failed to create or update session")
            Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel(BAD_REQUEST.toString, "Unable to createOrUpdate pension service")))
          case Right(_) =>
            Right(Ok)
        }
      case _ =>
        logger.info("[submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) => //TODO: investigate  the use of the previously used pensionSessionService.clear
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
