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

package controllers.pensions.paymentsIntoPension

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoPension.routes._
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
import views.html.pensions.WorkplaceAmountView

import javax.inject.Inject
import scala.concurrent.Future

class WorkplaceAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                          authAction: AuthorisedAction,
                                          workplaceAmountView: WorkplaceAmountView,
                                          appConfig: AppConfig,
                                          pensionSessionService: PensionSessionService,
                                          errorHandler: ErrorHandler,
                                          clock: Clock) extends FrontendController(cc) with I18nSupport {


  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.workplaceAmount.error.noEntry",
    wrongFormatKey = "pensions.workplaceAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.workplaceAmount.error.maxAmount"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion.contains(true)) {
          data.pensions.paymentsIntoPension.totalWorkplacePensionPayments match {
            case Some(amount) =>
              Future.successful(Ok(workplaceAmountView(amountForm.fill(amount), taxYear)))
            case None =>
              Future.successful(Ok(workplaceAmountView(amountForm, taxYear)))
          }
        } else {
          Future.successful(Redirect(WorkplacePensionController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(workplaceAmountView(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(totalWorkplacePensionPayments = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              Redirect(PaymentsIntoPensionsCYAController.show(taxYear))
            }
        }

      }
    )
  }
}
