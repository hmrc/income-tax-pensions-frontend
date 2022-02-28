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
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.RetirementAnnuityAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class RetirementAnnuityAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  retirementAnnuityAmountView: RetirementAnnuityAmountView,
                                                  appConfig: AppConfig,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.retirementAnnuityAmount.error.noEntry",
    wrongFormatKey = "pensions.retirementAnnuityAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.retirementAnnuityAmount.error.overMaximum"
  )


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        if (data.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion.contains(true)) {
          data.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments match {
            case Some(amount) =>
              Future.successful(Ok(retirementAnnuityAmountView(amountForm.fill(amount), taxYear)))
            case None => Future.successful(Ok(retirementAnnuityAmountView(amountForm, taxYear)))
          }
        } else {
          Future.successful(Redirect(RetirementAnnuityController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(retirementAnnuityAmountView(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel(PaymentsIntoPensionViewModel()))
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(totalRetirementAnnuityContractPayments = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              Redirect(WorkplacePensionController.show(taxYear))
            }
        }
      }
    )
  }

}
