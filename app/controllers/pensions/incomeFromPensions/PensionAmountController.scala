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
import controllers.pensions.incomeFromPensions.routes.PensionSchemeStartDateController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.{FormUtils, FormsProvider}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.HowMuchPensionDidYouGetPaidPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{indexCheckThenJourneyCheck, redirectForSchemeLoop, schemeIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.PensionAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionAmountController @Inject() (mcc: MessagesControllerComponents,
                                         authAction: AuthorisedAction,
                                         view: PensionAmountView,
                                         pensionSessionService: PensionSessionService,
                                         errorHandler: ErrorHandler,
                                         formsProvider: FormsProvider)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper
    with FormUtils {

  private def validateIndex(optIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Option[Int] =
    optIndex match {
      case Some(index) if pensionSchemesList.size > index => Some(index)
      case _                                              => None
    }

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          indexCheckThenJourneyCheck(data, pensionSchemeIndex, HowMuchPensionDidYouGetPaidPage, taxYear) { data =>
            val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.getUKPensionIncomes
            validateIndex(pensionSchemeIndex, pensionIncomesList) match {
              case Some(index) =>
                Future.successful(
                  Ok(
                    view(
                      formsProvider.pensionAmountForm(request.user).fill((pensionIncomesList(index).amount, pensionIncomesList(index).taxPaid)),
                      taxYear,
                      index)))
              case None => Future.successful(Redirect(redirectForSchemeLoop(pensionIncomesList, taxYear)))
            }
          }
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        indexCheckThenJourneyCheck(data, pensionSchemeIndex, HowMuchPensionDidYouGetPaidPage, taxYear) { data =>
          val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.getUKPensionIncomes
          validateIndex(pensionSchemeIndex, pensionIncomesList) match {
            case Some(index) =>
              formsProvider
                .pensionAmountForm(request.user)
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
                  amounts => {
                    val pensionsCYAModel: PensionsCYAModel     = data.pensions
                    val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions

                    val ukPensionModel: UkPensionIncomeViewModel        = viewModel.getUKPensionIncomes(index)
                    val updatedUkPensionModel: UkPensionIncomeViewModel = ukPensionModel.copy(amount = amounts._1, taxPaid = amounts._2)
                    val updatedList: List[UkPensionIncomeViewModel]     = viewModel.getUKPensionIncomes.updated(index, updatedUkPensionModel)
                    val updatedCyaModel: PensionsCYAModel =
                      pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = Some(updatedList)))
                    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                      errorHandler.internalServerError()) {

                      schemeIsFinishedCheck(updatedList, index, taxYear, PensionSchemeStartDateController.show(taxYear, Some(index)))
                    }
                  }
                )
            case None => Future.successful(Redirect(redirectForSchemeLoop(pensionIncomesList, taxYear)))
          }
        }
      case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

}
