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
import controllers.predicates.actions.ActionsProvider
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.IncomeFromOtherUkPensionsPages.SchemeSummaryPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.PensionSchemeSummaryView

import javax.inject.Inject
import scala.concurrent.Future

class PensionSchemeSummaryController @Inject() (view: PensionSchemeSummaryView, actionsProvider: ActionsProvider)(implicit
    val mcc: MessagesControllerComponents,
    appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      indexCheckThenJourneyCheck(userSessionDataRequest.pensionsUserData, pensionSchemeIndex, SchemeSummaryPage, taxYear) { data =>
        val scheme: UkPensionIncomeViewModel = data.pensions.incomeFromPensions.uKPensionIncomes(pensionSchemeIndex.getOrElse(0))
        Future.successful(Ok(view(taxYear, scheme, pensionSchemeIndex)))
      }
  }
}
