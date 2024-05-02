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

package controllers.pensions.paymentsIntoPensions

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.PaymentsIntoPensionPages.CheckYourAnswersPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{PensionSessionService, PensionsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                   pensionsService: PensionsService,
                                                   view: PaymentsIntoPensionsCYAView,
                                                   pensionSessionService: PensionSessionService,
                                                   errorHandler: ErrorHandler,
                                                   mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  // TODO it will be refactored in the next PRs
  def show(taxYear: Int): Action[AnyContent] = auditProvider.paymentsIntoPensionsViewAuditing(taxYear) async { implicit request =>
    val cyaData = request.sessionData
    if (!cyaData.pensions.paymentsIntoPension.isFinished) {
      val checkRedirect = journeyCheck(CheckYourAnswersPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoPension)))
      }
    } else {
      pensionSessionService.createOrUpdateSessionData(request.user, cyaData.pensions, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(Ok(view(taxYear, cyaData.pensions.paymentsIntoPension)))
    }
  }

  // TODO Business Question: Do we need to exclude the journey if answers NOs to some of the questions?
  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.paymentsIntoPensionsUpdateAuditing(taxYear.endYear) async { implicit request =>
    val answersFromSession = request.sessionData.pensions.paymentsIntoPension

    val res = pensionsService.upsertPaymentsIntoPensions(
      request.user,
      taxYear,
      answersFromSession
    )

    handleResult(errorHandler, taxYear, Journey.PaymentsIntoPensions, res)
  }
}
