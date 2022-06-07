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
import controllers.predicates.AuthorisedAction
import forms.PensionSchemeDateForm
import forms.PensionSchemeDateForm.PensionSchemeDateModel
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.DateTimeUtil.localDateTimeFormat
import views.html.pensions.incomeFromPensions.PensionSchemeStartDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future

class PensionSchemeStartDateController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                 appConfig: AppConfig,
                                                 authAction: AuthorisedAction,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 view: PensionSchemeStartDateView,
                                                 clock: Clock) extends FrontendController(mcc) with I18nSupport {

  private val form: Form[PensionSchemeDateModel] = PensionSchemeDateForm.pensionSchemeDateForm

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        validateIndex(pensionSchemeIndex, data.pensions.incomeFromPensions.uKPensionIncomes) match {
          case Some(index) =>
            data.pensions.incomeFromPensions.uKPensionIncomes(index).startDate match {
              case Some(startDate) =>
                val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
                val filledForm: Form[PensionSchemeDateModel] = form.fill(PensionSchemeDateModel(
                  parsedDate.getDayOfMonth.toString, parsedDate.getMonthValue.toString, parsedDate.getYear.toString)
                )
                Future.successful(Ok(view(filledForm, taxYear, index)))
              case None => Future.successful(Ok(view(form, taxYear, index)))
            }
          case None => Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      //TODO redirect to income from pensions CYA page
      case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        validateIndex(pensionSchemeIndex, data.pensions.incomeFromPensions.uKPensionIncomes) match {
          case Some(index) =>
            val verifiedForm = form.bindFromRequest()
            verifiedForm.copy(errors = PensionSchemeDateForm.verifyDate(verifiedForm.get)).fold(

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
      //TODO redirect to income from pensions CYA page
      case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  private def validateIndex(optIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Option[Int] = {
    optIndex match {
      case Some(index) if pensionSchemesList.size > index => Some(index)
      case _ => None
    }
  }
}
