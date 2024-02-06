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

import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.ActionsProvider
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.ShortServiceRefundsPages.RefundSchemesSummaryPage
import services.redirects.ShortServiceRefundsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.shortServiceRefunds.RefundSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RefundSummaryController @Inject() (actionsProvider: ActionsProvider,
                                         view: RefundSummaryView,
                                         pensionSessionService: PensionSessionService,
                                         errorHandler: ErrorHandler,
                                         mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    cleanUpSchemes(sessionUserData.pensionsUserData).flatMap {
      case Right(updatedUserData) =>
        val checkRedirect = journeyCheck(RefundSchemesSummaryPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) { _ =>
          Future.successful(Ok(view(taxYear, updatedUserData.pensions.shortServiceRefunds.refundPensionScheme)))
        }
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
    }
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData)(implicit ec: ExecutionContext): Future[Either[DatabaseError, PensionsUserData]] = {
    val schemes            = pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
    val filteredSchemes    = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel   = pensionsUserData.pensions.shortServiceRefunds.copy(refundPensionScheme = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(shortServiceRefunds = updatedViewModel)
    val updatedUserData    = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData).map(_.map(_ => updatedUserData))
  }
}
