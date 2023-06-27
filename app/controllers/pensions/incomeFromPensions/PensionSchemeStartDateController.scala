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
import controllers.pensions.incomeFromPensions.routes.PensionSchemeSummaryController
import controllers.predicates.ActionsProvider
import controllers.validatedIndex
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsRedirects.redirectForSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.DateTimeUtil.localDateTimeFormat
import views.html.pensions.incomeFromPensions.PensionSchemeStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeStartDateController @Inject()(pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 view: PensionSchemeStartDateView,
                                                 formProvider: FormsProvider
                                                )(implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  actionsProvider: ActionsProvider,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] =
    actionsProvider.userSessionDataFor(taxYear) { implicit sessionDataRequest =>
      val data = sessionDataRequest.pensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
      validatedIndex(pensionSchemeIndex, data.size) match {
        case Some(validIndex) =>
          data(pensionSchemeIndex.get).startDate.fold {
            Ok(view(formProvider.pensionSchemeDateForm, taxYear, validIndex))
          } { startDate =>
            val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
            val filledForm: Form[DateModel] = formProvider.pensionSchemeDateForm.fill(DateModel(
              parsedDate.getDayOfMonth.toString, parsedDate.getMonthValue.toString, parsedDate.getYear.toString
            ))
            Ok(view(filledForm, taxYear, validIndex))
          }
        case None => Redirect(redirectForSchemeLoop(data, taxYear))
      }
    }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] =
    actionsProvider.userSessionDataFor(taxYear) async { implicit sessionDataRequest =>
      val pensionIncomes = sessionDataRequest.pensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
      validatedIndex(pensionSchemeIndex, pensionIncomes.size) match {
        case Some(validIndex) =>
          val verifiedForm = formProvider.pensionSchemeDateForm.bindFromRequest()
          verifiedForm.copy(errors = DateForm.verifyDate(verifiedForm.get, "incomeFromPensions.pensionStartDate")).fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, taxYear, validIndex))),
            startDate => {
              val pensionsCYAModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions
              val viewModel = pensionsCYAModel.incomeFromPensions
              val pensionScheme: UkPensionIncomeViewModel = viewModel.uKPensionIncomes(validIndex)
              val newStartDate = startDate.toLocalDate.toString

              val updatedPensionIncomesList: Seq[UkPensionIncomeViewModel] = {
                viewModel.uKPensionIncomes.updated(validIndex, pensionScheme.copy(startDate = Some(newStartDate)))
              }
              val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = updatedPensionIncomesList))

              pensionSessionService.createOrUpdateSessionData(sessionDataRequest.user,
                updatedCyaModel, taxYear, sessionDataRequest.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(PensionSchemeSummaryController.show(taxYear, pensionSchemeIndex))
              }
            }
          )
        case _ => Future.successful(Redirect(redirectForSchemeLoop(pensionIncomes, taxYear)))
      }
    }
}
