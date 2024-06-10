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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionsService
import services.redirects.StatePensionPages.StatePensionsCYAPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import validation.pensions.incomeFromPensions.StatePensionValidator.validateFlow
import views.html.pensions.incomeFromPensions.StatePensionCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionCYAController @Inject() (mcc: MessagesControllerComponents,
                                           auditProvider: AuditActionsProvider,
                                           pensionsService: PensionsService,
                                           view: StatePensionCYAView,
                                           errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.incomeFromStatePensionsViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData    = request.sessionData
    val taxYearInt = taxYear.endYear

    if (cyaData.pensions.incomeFromPensions.isStatePensionFinished) {
      Future.successful(Ok(view(taxYearInt, cyaData.pensions.incomeFromPensions)))
    } else {
      val incomeFromPensions = request.sessionData.pensions.incomeFromPensions
      validateFlow(incomeFromPensions, StatePensionsCYAPage, taxYear.endYear) {
        Future.successful(Ok(view(taxYear.endYear, incomeFromPensions)))
      }
    }
  }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.incomeFromStatePensionsUpdateAuditing(taxYear.endYear) async { implicit request =>
    def upsertAnswers = pensionsService.upsertStatePension(
      request.user,
      taxYear,
      request.sessionData
    )(request.user.withDownstreamHc(hc), ec)

    // TODO Do we need this validate Flow? Why other journeys don't have it
    validateFlow(request.sessionData.pensions.incomeFromPensions, StatePensionsCYAPage, taxYear.endYear) {
      handleResult(errorHandler, taxYear, Journey.StatePension, upsertAnswers)
    }
  }

}
