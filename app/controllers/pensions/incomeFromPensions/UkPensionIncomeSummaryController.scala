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

import config.AppConfig
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.UkPensionIncomePage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.incomeFromPensions.UkPensionIncomeSummary

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkPensionIncomeSummaryController @Inject() (cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  ukPensionIncomeSummary: UkPensionIncomeSummary,
                                                  pensionSessionService: PensionSessionService)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val updatedUserData = cleanUpSchemes(data)
        val checkRedirect   = journeyCheck(UkPensionIncomePage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
          val incomeFromPensionList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.getUKPensionIncomes
          Future(Ok(ukPensionIncomeSummary(taxYear, incomeFromPensionList)))
        }
      case None => Future(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData): PensionsUserData = {
    val schemes            = pensionsUserData.pensions.incomeFromPensions.getUKPensionIncomes
    val filteredSchemes    = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel   = pensionsUserData.pensions.incomeFromPensions.copy(uKPensionIncomes = Some(filteredSchemes))
    val updatedPensionData = pensionsUserData.pensions.copy(incomeFromPensions = updatedViewModel)
    val updatedUserData    = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSession(updatedUserData)
    updatedUserData
  }
}
