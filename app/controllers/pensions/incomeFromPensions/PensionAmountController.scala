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
import controllers.pensions.incomeFromPensions.routes.{PensionSchemeStartDateController, UkPensionIncomeCYAController}
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.{FormUtils, FormsProvider}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsRedirects.redirectForSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.PensionAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        pensionAmountView: PensionAmountView,
                                        appConfig: AppConfig,
                                        pensionSessionService: PensionSessionService,
                                        errorHandler: ErrorHandler,
                                        formsProvider: FormsProvider,
                                        clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  private def validateIndex(optIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Option[Int] = {
    optIndex match {
      case Some(index) if pensionSchemesList.size > index => Some(index)
      case _ => None
    }
  }


  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes
        validateIndex(pensionSchemeIndex, pensionIncomesList) match {
          case Some(index) =>
            Future.successful(Ok(pensionAmountView(formsProvider.pensionAmountForm.fill((
              pensionIncomesList(index).amount,
              pensionIncomesList(index).taxPaid)),
              taxYear, index)))
          case None => Future.successful(Redirect(redirectForSchemeLoop(pensionIncomesList, taxYear)))
        }
      case _ =>
        Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
    }
  }


  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes
        validateIndex(pensionSchemeIndex, pensionIncomesList) match {
          case Some(index) =>
            formsProvider.pensionAmountForm.bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(pensionAmountView(formWithErrors, taxYear, index))),
              amounts => {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions

                val ukPensionModel: UkPensionIncomeViewModel = viewModel.uKPensionIncomes(index)
                val updatedUkPensionModel: UkPensionIncomeViewModel = ukPensionModel.copy(amount = amounts._1, taxPaid = amounts._2)
                val updatedList: Seq[UkPensionIncomeViewModel] = viewModel.uKPensionIncomes.updated(index, updatedUkPensionModel)
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(incomeFromPensions =
                    viewModel.copy(uKPensionIncomes = updatedList))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(PensionSchemeStartDateController.show(taxYear, Some(index)))
                }
              })
          case None => Future.successful(Redirect(redirectForSchemeLoop(pensionIncomesList, taxYear)))
        }
      case _ =>
        Future.successful(Redirect(UkPensionIncomeCYAController.show(taxYear)))
    }
  }

}
