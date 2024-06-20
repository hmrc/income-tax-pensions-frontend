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
import controllers.predicates.actions.ActionsProvider
import forms.standard.LocalDateFormProvider
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.WhenDidYouStartGettingPaymentsPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.incomeFromPensions.PensionSchemeStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeStartDateController @Inject() (pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: PensionSchemeStartDateView,
                                                  actionsProvider: ActionsProvider,
                                                  formProvider: LocalDateFormProvider,
                                                  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val form: Form[LocalDate] = formProvider("pensionStartDate")

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async {
    implicit request =>
      indexCheckThenJourneyCheck(request.sessionData, pensionSchemeIndex, WhenDidYouStartGettingPaymentsPage, taxYear) { data =>
        val viewModel                         = data.pensions.incomeFromPensions.getUKPensionIncomes
        val index                             = pensionSchemeIndex.getOrElse(0)
        val maybeStartDate: Option[LocalDate] = viewModel(index).startDate.map(LocalDate.parse(_))
        val filledForm: Form[LocalDate]       = maybeStartDate.fold(form)(form.fill)
        Future.successful(Ok(view(filledForm, taxYear, index)))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async {
    implicit request =>
      indexCheckThenJourneyCheck(request.sessionData, pensionSchemeIndex, WhenDidYouStartGettingPaymentsPage, taxYear) { data =>
        val index = pensionSchemeIndex.getOrElse(0)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
            startDate => {
              val pensionsCYAModel: PensionsCYAModel      = data.pensions
              val viewModel                               = pensionsCYAModel.incomeFromPensions
              val pensionScheme: UkPensionIncomeViewModel = viewModel.getUKPensionIncomes(index)

              val updatedPensionIncomesList: List[UkPensionIncomeViewModel] =
                viewModel.getUKPensionIncomes.updated(index, pensionScheme.copy(startDate = Some(startDate.toString)))
              val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = Some(updatedPensionIncomesList)))

              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                errorHandler.internalServerError()) {
                Redirect(routes.PensionSchemeSummaryController.show(taxYear, pensionSchemeIndex))
              }
            }
          )
      }
  }
}
