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

package controllers.pensions.shortServiceRefunds

import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.ShortServiceRefundsPages.RefundSchemesSummaryPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.validateFlow
import views.html.pensions.shortServiceRefunds.RefundSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RefundSummaryController @Inject() (actionsProvider: ActionsProvider, view: RefundSummaryView, mcc: MessagesControllerComponents)(implicit
    val appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateFlow(answers, RefundSchemesSummaryPage, taxYear)(
      Future.successful(Ok(view(taxYear, answers.refundPensionScheme)))
    )
  }
}
