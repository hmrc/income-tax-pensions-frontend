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
import controllers.pensions.incomeFromPensions.routes.{UkPensionIncomeCYAController, UkPensionIncomeSummaryController}
import controllers.predicates.AuthorisedAction
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.DateTimeUtil.localDateTimeFormat
import views.html.pensions.incomeFromPensions.PensionSchemeStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeStartDateController @Inject()(
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: PensionSchemeStartDateView,
                                                  formProvider: FormsProvider
                                                )(implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  authAction: AuthorisedAction,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        validateIndex(pensionSchemeIndex, data.pensions.incomeFromPensions.uKPensionIncomes) match {
          case Some(index) =>
            data.pensions.incomeFromPensions.uKPensionIncomes(index).startDate match {
              case Some(startDate) =>
                val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
                val filledForm: Form[DateModel] = formProvider.pensionSchemeDateForm.fill(DateModel(
                  parsedDate.getDayOfMonth.toString, parsedDate.getMonthValue.toString, parsedDate.getYear.toString)
                )
                Future.successful(Ok(view(filledForm, taxYear, index)))
              case None => Future.successful(Ok(view(formProvider.pensionSchemeDateForm, taxYear, index)))
            }
          case None => Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      case _ => Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        validateIndex(pensionSchemeIndex, data.pensions.incomeFromPensions.uKPensionIncomes) match {
          case Some(index) =>
            val verifiedForm = formProvider.pensionSchemeDateForm.bindFromRequest()
            verifiedForm.copy(errors = DateForm.verifyDate(verifiedForm.get, "incomeFromPensions.pensionStartDate")).fold(

              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
              startDate => {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel = pensionsCYAModel.incomeFromPensions
                val pensionScheme: UkPensionIncomeViewModel = viewModel.uKPensionIncomes(index)
                val newStartDate = startDate.toLocalDate.toString

                val updatedPensionIncomesList: Seq[UkPensionIncomeViewModel] = {
                  viewModel.uKPensionIncomes.updated(index, pensionScheme.copy(startDate = Some(newStartDate)))
                }

                val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = updatedPensionIncomesList))

                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(UkPensionIncomeSummaryController.show(taxYear))
                }
              }
            )
          case None => Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      case _ => Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
    }
  }

  private def validateIndex(optIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Option[Int] = {
    optIndex match {
      case Some(index) if pensionSchemesList.size > index => Some(index)
      case _ => None
    }
  }
}
