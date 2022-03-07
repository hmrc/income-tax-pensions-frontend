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

package controllers.pensions.annualAllowance

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.annual.AboveReducedAnnualAllowanceAmountView

import javax.inject.Inject
import scala.concurrent.Future

class AboveReducedAnnualAllowanceAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                            authAction: AuthorisedAction,
                                                            view: AboveReducedAnnualAllowanceAmountView,
                                                            appConfig: AppConfig,
                                                            pensionSessionService: PensionSessionService,
                                                            errorHandler: ErrorHandler,
                                                            clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {
  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.reducedAnnualAllowanceAmount.error.noEntry",
    wrongFormatKey = "pensions.reducedAnnualAllowanceAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.reducedAnnualAllowanceAmount.error.overMaximum"
  )


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion.contains(true)) {
          data.pensions.pensionsAnnualAllowances.aboveAnnualAllowance match {
            case Some(amount) =>
              Future.successful(Ok(view(amountForm.fill(amount), taxYear)))
            case None => Future.successful(Ok(view(amountForm, taxYear)))
          }
        } else {
          //TODO: redirect to "Above allowance question page?" page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel: PensionAnnualAllowancesViewModel = pensionsCYAModel.pensionsAnnualAllowances
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(aboveAnnualAllowance = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              //TODO: redirect to "Has your client's pension provider paid the annual allowance tax?" page
              Redirect(PensionsSummaryController.show(taxYear))
            }
        }
      }
    )
  }

}
