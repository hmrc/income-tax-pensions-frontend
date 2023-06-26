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
import controllers.pensions.paymentsIntoPensions.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoPensionPages.OneOffRasAmountPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.OneOffRASPaymentsAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class OneOffRASPaymentsAmountController @Inject()(authAction: AuthorisedAction,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: OneOffRASPaymentsAmountView,
                                                  formProvider: PaymentsIntoPensionFormProvider)
                                                 (implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      val checkRedirect = journeyCheck(OneOffRasAmountPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

        val viewModel = data.pensions.paymentsIntoPension

        (viewModel.oneOffRasPaymentPlusTaxReliefQuestion,
          viewModel.totalOneOffRasPaymentPlusTaxRelief,
          viewModel.totalRASPaymentsAndTaxRelief
        ) match {
          case (Some(true), amount, Some(rasAmount)) =>
            val form = amount.fold(formProvider.oneOffRASPaymentsAmountForm)(a => formProvider.oneOffRASPaymentsAmountForm.fill(a))
            Future.successful(Ok(view(form, taxYear, rasAmount)))
          case _ => errorHandler.futureInternalServerError()
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      optData =>
        val checkRedirect = journeyCheck(OneOffRasAmountPage, _, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
          formProvider.oneOffRASPaymentsAmountForm.bindFromRequest().fold(
            formWithErrors => {
              data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief.fold(
                Future.successful(Redirect(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)))
              )(rasAmount => Future.successful(BadRequest(view(formWithErrors, taxYear, rasAmount))))
            },
            amount => {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(
                  totalOneOffRasPaymentPlusTaxRelief = Some(amount), totalPaymentsIntoRASQuestion = None
                ))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(TotalPaymentsIntoRASController.show(taxYear))
              }
            }
          )
        }
    }
  }

}
