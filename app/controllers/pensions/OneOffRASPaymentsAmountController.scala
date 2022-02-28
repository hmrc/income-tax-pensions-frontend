/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.AmountForm
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.OneOffRASPaymentsAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class OneOffRASPaymentsAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  authAction: AuthorisedAction,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: OneOffRASPaymentsAmountView,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport {

  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "paymentsIntoPensions.oneOffRasAmount.error.noEntry",
    wrongFormatKey = "paymentsIntoPensions.oneOffRasAmount.error.invalidFormat",
    exceedsMaxAmountKey = "paymentsIntoPensions.oneOffRasAmount.error.overMaximum"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        val viewModel = data.pensions.paymentsIntoPension

        (viewModel.oneOffRasPaymentPlusTaxReliefQuestion,
          viewModel.totalOneOffRasPaymentPlusTaxRelief,
          viewModel.totalRASPaymentsAndTaxRelief
        ) match {
          case (Some(true), amount, Some(rasAmount)) =>
            val form = amount.fold(amountForm)(a => amountForm.fill(a))
            Future.successful(Ok(view(form, taxYear, rasAmount)))
          case (_, _, None) =>
            Future.successful(Redirect(controllers.pensions.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)))
          case _ =>
            //TODO - redirect to OneOffRASPayments page when available
            Future.successful(Redirect(controllers.pensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(controllers.pensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      data =>
        amountForm.bindFromRequest.fold(
          formWithErrors => {
            data.flatMap(_.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief).fold(
              Future.successful(Redirect(controllers.pensions.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)))
            )(rasAmount => Future.successful(BadRequest(view(formWithErrors, taxYear, rasAmount))))
          },
          amount => {
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel(PaymentsIntoPensionViewModel()))
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(totalOneOffRasPaymentPlusTaxRelief = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              // TODO - redirect to total payments into RAS pensions page when built
              Redirect(controllers.pensions.routes.PensionsTaxReliefNotClaimedController.show(taxYear))
            }
          }
        )
    }
  }

}
