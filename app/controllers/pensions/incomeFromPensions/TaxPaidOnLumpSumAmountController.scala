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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.incomeFromPensions.routes.{StatePensionLumpSumStartDateController, TaxPaidOnStatePensionLumpSumController}
import controllers.predicates.TaxYearAction.taxYearAction
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.TaxPaidOnLumpSumAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TaxPaidOnLumpSumAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 taxPaidOnLumpSumAmountView: TaxPaidOnLumpSumAmountView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 inYearAction: InYearAction,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {


  def amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "incomeFromPensions.taxPaidOnLumpSumAmount.error.error.noEntry",
    wrongFormatKey = "incomeFromPensions.taxPaidOnLumpSumAmount.error.error.incorrectFormat",
    exceedsMaxAmountKey = "incomeFromPensions.taxPaidOnLumpSumAmount.error.error.overMaximum"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          val preAmount = data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid)
          if (data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaidQuestion).contains(true)) {
            data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid) match {
              case Some(amount) => Future.successful(Ok(taxPaidOnLumpSumAmountView(amountForm.fill(amount), taxYear, preAmount)))
              case None => Future.successful(Ok(taxPaidOnLumpSumAmountView(amountForm, taxYear, None)))
            }
          } else {
            Future.successful(Redirect(TaxPaidOnStatePensionLumpSumController.show(taxYear)))
          }
        case _ =>
          //TODO redirect to the state pension CYA page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          amountForm.bindFromRequest().fold(
            formWithErrors => {
              val taxPaid = data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid)
              Future.successful(BadRequest(taxPaidOnLumpSumAmountView(formWithErrors, taxYear, taxPaid)))
            },
            amount => {
              if (data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaidQuestion).contains(true)) {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions
                val statePensionLumpSumModel: Option[StateBenefitViewModel] = viewModel.statePensionLumpSum
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(statePensionLumpSum = statePensionLumpSumModel.map(_.copy(taxPaid = Some(amount)))))
                }

                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(StatePensionLumpSumStartDateController.show(taxYear))
                }
              } else {
                Future.successful(Redirect(TaxPaidOnStatePensionLumpSumController.show(taxYear)))
              }
            }
          )
        case _ =>
          //TODO redirect to the state pension CYA page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }
}
