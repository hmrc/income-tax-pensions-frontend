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
import models.mongo.PensionsCYAModel
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionsService
import services.redirects.IncomeFromOtherUkPensionsPages.CheckUkPensionIncomeCYAPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.incomeFromPensions.UkPensionIncomeCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPensionIncomeCYAController @Inject() (mcc: MessagesControllerComponents,
                                              auditProvider: AuditActionsProvider,
                                              pensionsService: PensionsService,
                                              view: UkPensionIncomeCYAView,
                                              errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.ukPensionIncomeViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData    = request.sessionData
    val taxYearInt = taxYear.endYear

    if (cyaData.pensions.incomeFromPensions.isUkPensionFinished) {
      Future.successful(Ok(view(taxYearInt, cyaData.pensions.incomeFromPensions)))
    } else {
      val checkRedirect = journeyCheck(CheckUkPensionIncomeCYAPage, _: PensionsCYAModel, taxYear.endYear)
      redirectBasedOnCurrentAnswers(taxYear.endYear, Some(request.sessionData), cyaPageCall(taxYear.endYear))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear.endYear, data.pensions.incomeFromPensions)))
      }
    }
  }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.ukPensionIncomeUpdateAuditing(taxYear.endYear) async { implicit request =>
    val res = pensionsService.upsertUkPensionIncome(
      request.user,
      taxYear,
      request.sessionData
    )(request.user.withDownstreamHc(hc), ec)

    handleResult(errorHandler, taxYear, Journey.UkPensionIncome, res)
  }

}
