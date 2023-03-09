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
import controllers.pensions.incomeFromPensions.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.PensionSchemeDetailsForm.PensionSchemeDetailsModel
import forms.{FormUtils, PensionSchemeDetailsForm}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.PensionSchemeDetailsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeDetailsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               pensionSchemeDetailsView: PensionSchemeDetailsView,
                                               appConfig: AppConfig,
                                               pensionSessionService: PensionSessionService,
                                               errorHandler: ErrorHandler,
                                               clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes
        val hasPensionIncomes = data.pensions.incomeFromPensions.uKPensionIncomesQuestion.contains(true)
        val form: Form[PensionSchemeDetailsModel] = PensionSchemeDetailsForm.pensionSchemeDetailsForm

        if (validateIndex(pensionSchemeIndex, pensionIncomesList)) {
          (hasPensionIncomes, pensionIncomesList, pensionSchemeIndex) match {
            case (false, _, _) => Future.successful(Redirect(UkPensionSchemePaymentsController.show(taxYear)))

            case (_, list, Some(index)) =>
              val fillModel = PensionSchemeDetailsModel(
                list(index).pensionSchemeName.getOrElse(""), list(index).pensionSchemeRef.getOrElse(""), list(index).pensionId.getOrElse(""))
              Future.successful(Ok(pensionSchemeDetailsView(form.fill(fillModel), taxYear, pensionSchemeIndex)))

            case (_, _, _) => Future.successful(Ok(pensionSchemeDetailsView(form, taxYear, pensionSchemeIndex)))
          }
        } else {
          Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      case None =>
        Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    PensionSchemeDetailsForm.pensionSchemeDetailsForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionSchemeDetailsView(formWithErrors, taxYear, pensionSchemeIndex))),
      formModel => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            val pensionsCYAModel = data.pensions
            val viewModel = pensionsCYAModel.incomeFromPensions

            if (validateIndex(pensionSchemeIndex, viewModel.uKPensionIncomes)) {

              val newPensionIncomeScheme: Seq[UkPensionIncomeViewModel] = Seq(UkPensionIncomeViewModel(
                pensionSchemeName = Some(formModel.providerName),
                pensionSchemeRef = Some(formModel.schemeReference),
                pensionId = Some(formModel.pensionId))
              )

              val updatedPensionIncomesList: Seq[UkPensionIncomeViewModel] =
                (viewModel.uKPensionIncomes, pensionSchemeIndex) match {
                  case (list, Some(index)) =>
                    list.updated(index, list(index).copy(pensionSchemeName = Some(formModel.providerName),
                      pensionSchemeRef = Some(formModel.schemeReference), pensionId = Some(formModel.pensionId)))
                  case (list, None) => list ++ newPensionIncomeScheme
                }

              val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = updatedPensionIncomesList))

              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(PensionAmountController.show(taxYear, pensionSchemeIndex.orElse(Some(updatedPensionIncomesList.size - 1))))
              }
            } else {
              Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
            }
          case None => Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
        }
      })
  }

  private def validateIndex(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Boolean = {
    pensionSchemeIndex match {
      case Some(index) =>
        pensionSchemesList.size > index
      case _ =>
        true
    }
  }
}
