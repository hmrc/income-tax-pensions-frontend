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

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import controllers.validatedIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.RemoveReliefsSchemePage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.RemoveReliefSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveReliefSchemeController @Inject() (actionsProvider: ActionsProvider,
                                              pensionSessionService: PensionSessionService,
                                              view: RemoveReliefSchemeView,
                                              errorHandler: ErrorHandler,
                                              mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit sessionUserData =>
    indexCheckThenJourneyCheck(sessionUserData.sessionData, index, RemoveReliefsSchemePage, taxYear) { relief: Relief =>
      Future.successful(Ok(view(taxYear = taxYear, reliefSchemeList = List(relief), index = index)))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit sessionUserData =>
    val pensionReliefScheme = sessionUserData.sessionData.pensions.paymentsIntoOverseasPensions.reliefs
    validatedIndex(index, pensionReliefScheme.size)
      .fold(Future.successful(Redirect(ReliefsSchemeSummaryController.show(taxYear)))) { i =>
        indexCheckThenJourneyCheck(sessionUserData.sessionData, Some(i), RemoveReliefsSchemePage, taxYear) { _ =>
          val updatedPensionReliefScheme = pensionReliefScheme.patch(i, Nil, 1)
          updateSessionData(sessionUserData.sessionData, updatedPensionReliefScheme, taxYear)
        }
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, reliefScheme: Seq[Relief], taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {
    val updatedCyaModel: PensionsCYAModel =
      pensionUserData.pensions.copy(paymentsIntoOverseasPensions = pensionUserData.pensions.paymentsIntoOverseasPensions.copy(reliefs = reliefScheme))

    pensionSessionService.createOrUpdateSessionData[Result](request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(ReliefsSchemeSummaryController.show(taxYear))
    }
  }
}
