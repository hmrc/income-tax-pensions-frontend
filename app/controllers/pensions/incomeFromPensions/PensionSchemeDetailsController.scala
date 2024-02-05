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
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.PensionSchemeDetailsForm.PensionSchemeDetailsModel
import forms.{FormUtils, PensionSchemeDetailsForm}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.PensionSchemeDetailsPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{indexCheckThenJourneyCheck, schemeIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.PensionSchemeDetailsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeDetailsController @Inject() (implicit
    val mcc: MessagesControllerComponents,
    authAction: AuthorisedAction,
    view: PensionSchemeDetailsView,
    appConfig: AppConfig,
    pensionSessionService: PensionSessionService,
    errorHandler: ErrorHandler,
    clock: Clock)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper
    with FormUtils {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          indexCheckThenJourneyCheck(data, pensionSchemeIndex, PensionSchemeDetailsPage, taxYear) { data =>
            val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes
            val form: Form[PensionSchemeDetailsModel]             = PensionSchemeDetailsForm.pensionSchemeDetailsForm(request.user)

            pensionSchemeIndex match {
              case Some(index) =>
                val fillModel = PensionSchemeDetailsModel(
                  pensionIncomesList(index).pensionSchemeName.getOrElse(""),
                  pensionIncomesList(index).pensionSchemeRef.getOrElse(""),
                  pensionIncomesList(index).pensionId.getOrElse("")
                )
                Future.successful(Ok(view(form.fill(fillModel), taxYear, pensionSchemeIndex)))
              case _ =>
                Future.successful(Ok(view(form, taxYear, pensionSchemeIndex)))
            }
          }
        case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    PensionSchemeDetailsForm
      .pensionSchemeDetailsForm(request.user)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, pensionSchemeIndex))),
        formModel =>
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            case Some(data) =>
              indexCheckThenJourneyCheck(data, pensionSchemeIndex, PensionSchemeDetailsPage, taxYear) { data =>
                val pensionsCYAModel = data.pensions
                val viewModel        = pensionsCYAModel.incomeFromPensions

                val newPensionIncomeScheme: Seq[UkPensionIncomeViewModel] = Seq(
                  UkPensionIncomeViewModel(
                    pensionSchemeName = Some(formModel.providerName),
                    pensionSchemeRef = Some(formModel.schemeReference),
                    pensionId = Some(formModel.pensionId)))

                val updatedPensionIncomesList: Seq[UkPensionIncomeViewModel] =
                  (viewModel.uKPensionIncomes, pensionSchemeIndex) match {
                    case (list, Some(index)) =>
                      list.updated(
                        index,
                        list(index).copy(
                          pensionSchemeName = Some(formModel.providerName),
                          pensionSchemeRef = Some(formModel.schemeReference),
                          pensionId = Some(formModel.pensionId))
                      )
                    case (list, None) => list ++ newPensionIncomeScheme
                  }

                val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = updatedPensionIncomesList))
                val updatedPensionSchemeIndex =
                  (viewModel.uKPensionIncomes, pensionSchemeIndex) match {
                    case (list, None) if list.isEmpty => Some(0)
                    case (list, None)                 => Some(list.size)
                    case (_, Some(_))                 => pensionSchemeIndex
                  }

                pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  schemeIsFinishedCheck(
                    updatedPensionIncomesList,
                    updatedPensionSchemeIndex.getOrElse(0),
                    taxYear,
                    PensionAmountController.show(taxYear, updatedPensionSchemeIndex))
                }
              }

            case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
      )
  }
}
