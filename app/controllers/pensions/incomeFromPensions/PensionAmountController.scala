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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromPensions.routes.UkPensionIncomeSummaryController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.TaxYearAction.taxYearAction
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{FormUtils, TupleAmountForm}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.PensionAmountView

import javax.inject.Inject
import scala.concurrent.Future

class PensionAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        pensionAmountView: PensionAmountView,
                                        appConfig: AppConfig,
                                        pensionSessionService: PensionSessionService,
                                        inYearAction: InYearAction,
                                        errorHandler: ErrorHandler,
                                        clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm: Form[(Option[BigDecimal], Option[BigDecimal])] = TupleAmountForm.amountForm(
    emptyFieldKey1 = "pensions.pensionAmount.totalTax.error.noEntry",
    wrongFormatKey1 = s"pensions.pensionAmount.totalTax.error.incorrectFormat",
    exceedsMaxAmountKey1 = s"pensions.pensionAmount.totalTax.error.overMaximum",
    emptyFieldKey2 = s"pensions.pensionAmount.taxPaid.error.noEntry",
    wrongFormatKey2 = s"pensions.pensionAmount.taxPaid.error.incorrectFormat",
    exceedsMaxAmountKey2 = s"pensions.pensionAmount.taxPaid.error.overMaximum"
  )

  private def validateIndex(pensionSchemeIndex: Int, pensionSchemesList: Seq[UkPensionIncomeViewModel]): Boolean = {
    pensionSchemesList.size > pensionSchemeIndex
  }


  def show(taxYear: Int, pensionSchemeIndex: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes

        if (validateIndex(pensionSchemeIndex, pensionIncomesList)) {
          val totalTaxOpt = pensionIncomesList(pensionSchemeIndex).amount
          val taxPaidOpt = pensionIncomesList(pensionSchemeIndex).taxPaid

          (totalTaxOpt, taxPaidOpt) match {
            case (Some(totalTax), Some(taxPaid)) =>
              Future.successful(Ok(pensionAmountView(amountForm.fill((Some(totalTax), Some(taxPaid))), taxYear, pensionSchemeIndex)))
            case (Some(totalTax), None) =>
              Future.successful(Ok(pensionAmountView(amountForm.fill((Some(totalTax), None)), taxYear, pensionSchemeIndex)))
            case (_, _) =>
              Future.successful(Ok(pensionAmountView(amountForm, taxYear, pensionSchemeIndex)))
          }

        }
        else {
          Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      case _ =>
        //TODO: - cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  def submit(taxYear: Int, pensionSchemeIndex: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(pensionAmountView(formWithErrors, taxYear, pensionSchemeIndex))),
      amounts => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions

            if (validateIndex(pensionSchemeIndex, viewModel.uKPensionIncomes)) {

              val ukPensionModel: UkPensionIncomeViewModel = viewModel.uKPensionIncomes(pensionSchemeIndex)
              val updatedUkPensionModel: UkPensionIncomeViewModel = ukPensionModel.copy(amount = amounts._1, taxPaid = amounts._2)
              val updatedList: Seq[UkPensionIncomeViewModel] = viewModel.uKPensionIncomes.updated(pensionSchemeIndex, updatedUkPensionModel)
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(incomeFromPensions =
                  viewModel.copy(uKPensionIncomes = updatedList))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                // todo: Page should redirect to next appropriate page when flow is confirmed
                Redirect(PensionsSummaryController.show(taxYear))
              }

            } else {
              Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
            }
          case _ =>
            //TODO: redirect to the income from pensions CYA page
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }

}
