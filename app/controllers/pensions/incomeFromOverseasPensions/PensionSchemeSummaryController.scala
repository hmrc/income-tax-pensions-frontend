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
import controllers.pensions.incomeFromOverseasPensions.routes.CountrySummaryListController
import controllers.predicates.actions.ActionsProvider
import models.pension.pages.OverseasPensionSchemeSummaryPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.IncomeFromOverseasPensionsPages.PensionSchemeSummaryPage
import services.redirects.IncomeFromOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromOverseasPensions.PensionsSchemeSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeSummaryController @Inject() (actionsProvider: ActionsProvider, pageView: PensionsSchemeSummary, cc: MessagesControllerComponents)(
    implicit appConfig: AppConfig)
    extends FrontendController(cc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, index, PensionSchemeSummaryPage, taxYear) { data =>
      Future.successful(Ok(pageView(OverseasPensionSchemeSummaryPage.apply(taxYear, data, index))))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, index, PensionSchemeSummaryPage, taxYear) { _ =>
      Future.successful(Redirect(CountrySummaryListController.show(taxYear)))
    }
  }
}
