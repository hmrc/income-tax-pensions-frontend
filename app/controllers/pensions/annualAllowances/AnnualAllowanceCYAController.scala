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

package controllers.pensions.annualAllowances

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionsService
import services.redirects.AnnualAllowancesPages.CYAPage
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.annualAllowances.AnnualAllowancesCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnnualAllowanceCYAController @Inject() (auditProvider: AuditActionsProvider,
                                              view: AnnualAllowancesCYAView,
                                              service: PensionsService,
                                              errorHandler: ErrorHandler,
                                              cc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.annualAllowancesViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData = request.sessionData

    if (cyaData.pensions.pensionsAnnualAllowances.isFinished) {
      Future.successful(Ok(view(taxYear, cyaData.pensions.pensionsAnnualAllowances)))
    } else {
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear.endYear)
      redirectBasedOnCurrentAnswers(taxYear.endYear, Some(cyaData), cyaPageCall(taxYear.endYear))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear, data.pensions.pensionsAnnualAllowances)))
      }
    }
  }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.annualAllowancesUpdateAuditing(taxYear.endYear) async { implicit request =>
    val res = service.upsertAnnualAllowances(
      request.user,
      taxYear,
      request.sessionData
    )(request.user.withDownstreamHc(hc), ec)

    handleResult(errorHandler, taxYear, Journey.AnnualAllowances, res)
  }
}
