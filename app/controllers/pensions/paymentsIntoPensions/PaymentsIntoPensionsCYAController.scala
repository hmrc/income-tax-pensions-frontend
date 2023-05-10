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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.{APIErrorBodyModel, APIErrorModel, AuthorisationRequest, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SimpleRedirectService.{PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}
import services.{ExcludeJourneyService, PensionReliefsService, PensionSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.RasAmountPage
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsCYAController @Inject()(authAction: AuthorisedAction,
                                                  view: PaymentsIntoPensionsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  pensionReliefsService: PensionReliefsService,
                                                  errorHandler: ErrorHandler,
                                                  excludeJourneyService: ExcludeJourneyService
                                                 )(implicit val mcc: MessagesControllerComponents,
                                                   appConfig: AppConfig,
                                                   clock: Clock) extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>

      (cya, prior) match {
        case (Some(cyaData), optionalPriorData) if !cyaData.pensions.paymentsIntoPension.isFinished =>
          val checkRedirect = PaymentsIntoPensionsRedirects.journeyCheck(RasAmountPage, _, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, cya)(checkRedirect) { data =>
            Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoPension)))
          }
        case (Some(cyaData), optionalPriorData) => pensionSessionService.createOrUpdateSessionData(request.user,
          cyaData.pensions, taxYear, isPriorSubmission = false)(
          errorHandler.internalServerError())(
          Ok(view(taxYear, cyaData.pensions.paymentsIntoPension))
        )
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user,
            cyaModel, taxYear, isPriorSubmission = false)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.paymentsIntoPension))
          )
        case _ => Future.successful(Redirect(controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        //todo Edem remove gateway question
        if (model.pensions.paymentsIntoPension.gateway.contains(false)) {
          excludeJourneyService.excludeJourney("pensions", taxYear, request.user.nino)(request.user, hc)
        }.flatMap {
          case Right(_) => performSubmission(taxYear, cya)(request.user, hc, request, clock)
          case Left(_) => errorHandler.futureInternalServerError()
        }
        if (!comparePriorData(model.pensions, prior)) {
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
        } else {
          performSubmission(taxYear, cya)(request.user, hc, request, clock)
        }
      }
    }
  }

  private def performSubmission(taxYear: Int, cya: Option[PensionsUserData]
                               )(implicit user: User,
                                 hc: HeaderCarrier,
                                 request: AuthorisationRequest[AnyContent],
                                 clock: Clock): Future[Result] = {

    (cya match {
      case Some(cyaData) =>
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
      case Right(_) =>
        pensionSessionService.clear(taxYear)(errorHandler.internalServerError())(
          Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
        )
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

//  private def redirects(cya: PensionsCYAModel, taxYear: Int): Either[Result, Unit] = {
//    PaymentsIntoPensionsRedirects.journeyCheck(CheckYourAnswersPage, cya, taxYear)
//  }

}
