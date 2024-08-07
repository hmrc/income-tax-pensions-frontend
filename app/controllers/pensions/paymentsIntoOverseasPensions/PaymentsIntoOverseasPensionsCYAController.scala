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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionsService
import services.redirects.PaymentsIntoOverseasPensionsPages.PaymentsIntoOverseasPensionsCYAPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.paymentsIntoOverseasPensions.PaymentsIntoOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoOverseasPensionsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                           view: PaymentsIntoOverseasPensionsCYAView,
                                                           errorHandler: ErrorHandler,
                                                           pensionsService: PensionsService,
                                                           mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.paymentsIntoOverseasPensionsViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData    = request.sessionData
    val taxYearInt = taxYear.endYear

    if (cyaData.pensions.paymentsIntoOverseasPensions.isFinished) {
      Future.successful(Ok(view(taxYear, cyaData.pensions.paymentsIntoOverseasPensions)))
    } else {
      val checkRedirect = journeyCheck(PaymentsIntoOverseasPensionsCYAPage, _: PensionsCYAModel, taxYearInt)
      redirectBasedOnCurrentAnswers(taxYearInt, Some(cyaData), cyaPageCall(taxYearInt))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoOverseasPensions)))
      }
    }
  }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.paymentsIntoOverseasPensionsUpdateAuditing(taxYear.endYear) async {
    implicit request =>
      val res = pensionsService.upsertPaymentsIntoOverseasPensions(
        request.user,
        taxYear,
        request.sessionData
      )(request.user.withDownstreamHc(hc), ec)

      handleResult(errorHandler, taxYear, Journey.PaymentsIntoOverseasPensions, res)
  }

}
