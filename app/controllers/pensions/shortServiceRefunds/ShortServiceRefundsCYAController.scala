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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import connectors.ContentHttpReads.logger
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionsService
import services.redirects.ShortServiceRefundsPages.CYAPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Logging, SessionHelper}
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.validateFlow
import views.html.pensions.shortServiceRefunds.ShortServiceRefundsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShortServiceRefundsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                  view: ShortServiceRefundsCYAView,
                                                  pensionService: PensionsService,
                                                  errorHandler: ErrorHandler,
                                                  mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.shortServiceRefundsViewAuditing(taxYear.endYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateFlow(answers, CYAPage, taxYear.endYear) {
      Future.successful(Ok(view(taxYear, answers)))
    }
  }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.shortServiceRefundsUpdateAuditing(taxYear.endYear) async { implicit request =>
    val res = pensionService.upsertShortServiceRefunds(
      request.user,
      taxYear,
      request.sessionData
    )(request.user.withDownstreamHc(hc), ec)

    handleResult(errorHandler, taxYear, Journey.ShortServiceRefunds, res)
  }
}
