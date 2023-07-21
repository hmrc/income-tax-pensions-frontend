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
import controllers.pensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.CYAPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import services.{PensionChargesService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsCYAController @Inject()(authAction: AuthorisedAction,
                                                  view: UnauthorisedPaymentsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  pensionChargesService: PensionChargesService,
                                                  errorHandler: ErrorHandler)
                                                 (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>


    def cyaDataIsEmpty(priorData: AllPensionsData): Future[Result] = {
      val cyaModel = generateCyaFromPrior(priorData)
      pensionSessionService.createOrUpdateSessionData(request.user,
        cyaModel, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(
        Ok(view(taxYear, cyaModel.unauthorisedPayments)))
    }

    def unauthorisedPaymentsCYAExists(cya: UnauthorisedPaymentsViewModel): Future[Result] = {
      Future.successful(Ok(view(taxYear, cya)))
    }


    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), Some(priorData: AllPensionsData)) if data.pensions.unauthorisedPayments.isEmpty =>
          cyaDataIsEmpty(priorData)
        case (Some(data), _) =>
          val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, cya, cyaPageCall(taxYear))(checkRedirect) { data =>
            unauthorisedPaymentsCYAExists(data.pensions.unauthorisedPayments)
          }
        case (None, Some(priorData)) =>
          cyaDataIsEmpty(priorData)
        case (None, None) =>
          val emptyUnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel(surchargeQuestion = None, noSurchargeQuestion = None)
          Future.successful(Ok(view(taxYear, emptyUnauthorisedPaymentsViewModel)))
        case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      ) { model =>
        val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(model), cyaPageCall(taxYear))(checkRedirect) { data =>

          //TODO: missing the comparison of session with Prior data
          pensionChargesService.saveUnauthorisedViewModel(request.user, taxYear).map {
            case Left(_) => errorHandler.internalServerError()
            case Right(_) => Redirect(PensionsSummaryController.show(taxYear))
          }
        }
      }
    }
  }

}
