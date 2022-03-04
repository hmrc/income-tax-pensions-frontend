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
import controllers.pensions.routes._
import controllers.predicates.AuthorisedAction
import forms.AmountForm
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.ReliefAtSourcePaymentsAndTaxReliefAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class ReliefAtSourcePaymentsAndTaxReliefAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                                   appConfig: AppConfig,
                                                                   authAction: AuthorisedAction,
                                                                   pensionSessionService: PensionSessionService,
                                                                   errorHandler: ErrorHandler,
                                                                   view: ReliefAtSourcePaymentsAndTaxReliefAmountView,
                                                                   clock: Clock) extends FrontendController(mcc) with I18nSupport {

  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.noEntry",
    wrongFormatKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.invalidFormat",
    exceedsMaxAmountKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.overMaximum"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.paymentsIntoPension.rasPensionPaymentQuestion.contains(true)) {
          data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief match {
            case Some(amount) => Future.successful(Ok(view(amountForm.fill(amount), taxYear)))
            case None => Future.successful(Ok(view(amountForm, taxYear)))
          }
        } else {
          Future.successful(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(
              PensionsCYAModel(PaymentsIntoPensionViewModel(), PensionAnnualAllowancesViewModel()))
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(totalRASPaymentsAndTaxRelief = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              Redirect(ReliefAtSourceOneOffPaymentsController.show(taxYear))
            }
        }

      }
    )
  }

}
