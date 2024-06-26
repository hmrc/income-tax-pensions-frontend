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

package controllers.pensions

import common.TaxYear
import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import models.pension.Journey
import models.pension.Journey.OverseasPensionsSummary
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.OverseasPensionsSummaryView

import javax.inject.{Inject, Singleton}

@Singleton
class OverseasPensionsSummaryController @Inject() (mcc: MessagesControllerComponents,
                                                   actionProvider: ActionsProvider,
                                                   pensionSessionService: PensionSessionService,
                                                   overseasPensionsSummaryView: OverseasPensionsSummaryView)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] =
    actionProvider.authoriseWithSessionAndPriorAuthRequest(TaxYear(taxYear), Journey.OverseasPensionsSummary).async { implicit request =>
      def summaryViewResult(taxYear: Int, pensionsSummary: HtmlContent) = Ok(overseasPensionsSummaryView(taxYear, pensionsSummary))

      pensionSessionService.mergePriorDataToSession(OverseasPensionsSummary, taxYear, request.user, summaryViewResult)
    }
}
