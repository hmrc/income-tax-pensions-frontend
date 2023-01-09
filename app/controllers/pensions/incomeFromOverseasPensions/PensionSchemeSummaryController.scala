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
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.ActionsProvider
import models.pension.pages.OverSeaPensionSchemeSummaryPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromOverseasPensions.PensionSchemeSummary

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PensionSchemeSummaryController @Inject()(actionsProvider: ActionsProvider,
                                                                       pageView:  PensionSchemeSummary,
                                                          pensionSessionService: PensionSessionService,
                                              )
                                                                      (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit sessionUserData =>
    sessionUserData.optPensionsUserData match {
      case Some(pensionsUserData) => Ok(pageView(OverSeaPensionSchemeSummaryPage.apply (taxYear, pensionsUserData, index)))
      case _ => Redirect(PensionsSummaryController.show(taxYear)) //todo redirect overseas summary page
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear)  { implicit sessionUserData =>
    sessionUserData.optPensionsUserData match {
      case Some(pensionsUserData) => Redirect(CountrySummaryListController.show(taxYear))
      case _ => Redirect(PensionsSummaryController.show(taxYear)) //todo redirect overseas summary page
    }
  }
}
