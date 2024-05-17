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
import controllers.pensions.incomeFromPensions.routes.UkPensionIncomeSummaryController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.RemovePensionIncomePage
import services.redirects.IncomeFromOtherUkPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.RemovePensionSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePensionSchemeController @Inject() (mcc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               view: RemovePensionSchemeView,
                                               pensionSessionService: PensionSessionService,
                                               errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          indexCheckThenJourneyCheck(data, pensionSchemeIndex, RemovePensionIncomePage, taxYear) { data =>
            val scheme: UkPensionIncomeViewModel = data.pensions.incomeFromPensions.uKPensionIncomes(pensionSchemeIndex.getOrElse(0))
            Future.successful(Ok(view(taxYear, scheme.pensionSchemeName.getOrElse(""), pensionSchemeIndex)))
          }
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        indexCheckThenJourneyCheck(data, pensionSchemeIndex, RemovePensionIncomePage, taxYear) { data =>
          val pensionsCYAModel                                   = data.pensions
          val viewModel                                          = pensionsCYAModel.incomeFromPensions
          val pensionIncomesList: List[UkPensionIncomeViewModel] = viewModel.uKPensionIncomes

          val updatedPensionIncomesList: List[UkPensionIncomeViewModel] = pensionIncomesList.patch(pensionSchemeIndex.get, Nil, 1)
          val updatedCyaModel = pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(uKPensionIncomes = updatedPensionIncomesList))

          pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
            errorHandler.internalServerError()) {
            Redirect(UkPensionIncomeSummaryController.show(taxYear))
          }
        }
      case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

}
