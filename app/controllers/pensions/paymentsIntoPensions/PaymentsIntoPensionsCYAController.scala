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
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionsService
import services.redirects.PaymentsIntoPensionPages.CheckYourAnswersPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                   pensionsService: PensionsService,
                                                   view: PaymentsIntoPensionsCYAView,
                                                   errorHandler: ErrorHandler,
                                                   mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.paymentsIntoPensionsViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData    = request.sessionData
    val taxYearInt = taxYear.endYear

    if (cyaData.pensions.paymentsIntoPension.isFinished) {
      Future.successful(Ok(view(taxYearInt, cyaData.pensions.paymentsIntoPension)))
    } else {
      val checkRedirect = journeyCheck(CheckYourAnswersPage, _, taxYearInt)
      redirectBasedOnCurrentAnswers(taxYearInt, Some(cyaData), cyaPageCall(taxYearInt))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYearInt, data.pensions.paymentsIntoPension)))
      }
    }
  }

  // TODO Business Question: Do we need to exclude the journey if answers NOs to some of the questions?
  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.paymentsIntoPensionsUpdateAuditing(taxYear.endYear) async { implicit request =>
    val res = pensionsService.upsertPaymentsIntoPensions(
      request.user,
      taxYear,
      request.sessionData
    )(request.user.withDownstreamHc(hc), executionContext)

    handleResult(errorHandler, taxYear, Journey.PaymentsIntoPensions, res)
  }
}
