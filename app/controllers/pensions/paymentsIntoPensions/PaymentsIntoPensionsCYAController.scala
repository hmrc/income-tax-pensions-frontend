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

package controllers.pensions.paymentsIntoPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.requests.UserPriorAndSessionDataRequest
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.PaymentsIntoPensionPages.CheckYourAnswersPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{ExcludeJourneyService, PensionReliefsService, PensionSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsCYAController @Inject()(auditProvider: AuditActionsProvider,
                                                  view: PaymentsIntoPensionsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  pensionReliefsService: PensionReliefsService,
                                                  errorHandler: ErrorHandler,
                                                  excludeJourneyService: ExcludeJourneyService)
                                                 (implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = auditProvider.paymentsIntoPensionsViewAuditing(taxYear) async { implicit sessionDataRequest =>
    val cyaData = sessionDataRequest.pensionsUserData
    if (!cyaData.pensions.paymentsIntoPension.isFinished) {
      val checkRedirect = journeyCheck(CheckYourAnswersPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoPension)))
      }
    } else {
      pensionSessionService.createOrUpdateSessionData(sessionDataRequest.user, cyaData.pensions, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(Ok(view(taxYear, cyaData.pensions.paymentsIntoPension)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.paymentsIntoPensionsUpdateAuditing(taxYear) async { implicit priorAndSessionRequest =>
    val (cya, prior, pIP) = (priorAndSessionRequest.pensionsUserData, priorAndSessionRequest.pensions,
      priorAndSessionRequest.pensionsUserData.pensions.paymentsIntoPension)

    if (!pIP.rasPensionPaymentQuestion.exists(x => x) && !pIP.pensionTaxReliefNotClaimedQuestion.exists(x => x)) {
      //TODO: check conditions for excluding Pensions from submission without gateway
      excludeJourneyService.excludeJourney("pensions", taxYear, priorAndSessionRequest.user.nino)(priorAndSessionRequest.user, hc)
    }.flatMap {
      case Right(_) => performSubmission(taxYear, Some(cya))(priorAndSessionRequest.user, hc, priorAndSessionRequest, clock)
      case Left(_) => errorHandler.futureInternalServerError()
    }
    if (!comparePriorData(cya.pensions, prior)) {
      Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
    } else {
      performSubmission(taxYear, Some(cya))(priorAndSessionRequest.user, hc, priorAndSessionRequest, clock)
    }
  }

  private def performSubmission(taxYear: Int, cya: Option[PensionsUserData]
                               )(implicit user: User,
                                 hc: HeaderCarrier,
                                 request: UserPriorAndSessionDataRequest[AnyContent],
                                 clock: Clock): Future[Result] = {
    (cya match {
      case Some(_) =>
        pensionReliefsService.persistPaymentIntoPensionViewModel(user, taxYear) map {
          case Left(_) =>
            logger.info("[PaymentIntoPensionsCYAController][submit] Failed to create or update session")
            Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel(BAD_REQUEST.toString, "Unable to createOrUpdate pension service")))
          case Right(_) =>
            Right(Ok)
        }
      case _ =>
        logger.info("[PaymentIntoPensionsCYAController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) => //TODO: investigate  the use of the previously used pensionSessionService.clear
        Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
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
