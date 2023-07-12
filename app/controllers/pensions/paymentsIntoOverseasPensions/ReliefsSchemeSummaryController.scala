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

package controllers.pensions.paymentsIntoOverseasPensions

import config.AppConfig
import controllers.predicates.ActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.ReliefsSchemeSummaryPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoOverseasPensions.ReliefSchemeSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReliefsSchemeSummaryController @Inject()(view: ReliefSchemeSummaryView, actionsProvider: ActionsProvider,
                                               pensionSessionService: PensionSessionService)
                                              (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      val updatedUserData = cleanUpReliefs(userSessionDataRequest.pensionsUserData)
      val checkRedirect = journeyCheck(ReliefsSchemeSummaryPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) { _ =>
        Future.successful(Ok(view(taxYear, updatedUserData.pensions.paymentsIntoOverseasPensions.reliefs)))
      }
  }

  private def cleanUpReliefs(pensionsUserData: PensionsUserData): PensionsUserData = {
    val reliefs = pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
    val filteredReliefs = if (reliefs.nonEmpty) reliefs.filter(relief => relief.isFinished) else reliefs
    val updatedViewModel = pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(reliefs = filteredReliefs)
    val updatedPensionData = pensionsUserData.pensions.copy(paymentsIntoOverseasPensions = updatedViewModel)
    val updatedUserData = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData)
    updatedUserData
  }
}
