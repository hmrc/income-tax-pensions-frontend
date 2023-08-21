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

package controllers.pensions.transferIntoOverseasPensions


import config.AppConfig
import models.mongo.PensionsCYAModel
import controllers.predicates.actions.ActionsProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.SchemesPayingTransferChargesSummary
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.TransferChargeSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TransferChargeSummaryController @Inject()(actionsProvider: ActionsProvider, view: TransferChargeSummaryView)
                                            (implicit mcc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataForInYear(taxYear) async {
    implicit sessionUserData =>
      val checkRedirect = journeyCheck(SchemesPayingTransferChargesSummary, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionUserData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data => {
          Future.successful(Ok(view(taxYear, data.pensions.transfersIntoOverseasPensions.transferPensionScheme)))
        }
    }
  }
}
