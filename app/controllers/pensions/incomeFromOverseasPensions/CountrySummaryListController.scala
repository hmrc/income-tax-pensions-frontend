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

package controllers.pensions.incomeFromOverseasPensions


import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsPages.CountrySchemeSummaryListPage
import services.redirects.IncomeFromOverseasPensionsRedirects.journeyCheck
import services.redirects.IncomeFromStatePensionsRedirects.cyaPageCall
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromOverseasPensions.CountrySummaryList

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CountrySummaryListController @Inject()(actionsProvider: ActionsProvider, countrySummary: CountrySummaryList,
                                             pensionSessionService: PensionSessionService)
                                            (implicit mcc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val updatedUserData = cleanUpSchemes(sessionUserData.pensionsUserData)
    val checkRedirect = journeyCheck(CountrySchemeSummaryListPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) { _ =>
      Future.successful(Ok(countrySummary(taxYear, updatedUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes)))
    }
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData): PensionsUserData = {
    val schemes = pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
    val filteredSchemes = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel = pensionsUserData.pensions.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(incomeFromOverseasPensions = updatedViewModel)
    val updatedUserData = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData)
    updatedUserData
  }
}
