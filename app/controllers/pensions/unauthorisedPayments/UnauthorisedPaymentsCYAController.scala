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

package controllers.pensions.unauthorisedPayments

import cats.data.EitherT
import cats.implicits._
import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.handleResult
import controllers.predicates.auditActions.AuditActionsProvider
import models.APIErrorModel
import models.domain.ApiResultT
import models.mongo.PensionsCYAModel
import models.pension.Journey
import models.pension.charges.UnauthorisedPaymentsViewModel
import models.requests.UserPriorAndSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.CYAPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import services.{ExcludeJourneyService, PensionsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Logging
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                   view: UnauthorisedPaymentsCYAView,
                                                   pensionsService: PensionsService,
                                                   errorHandler: ErrorHandler,
                                                   excludeJourneyService: ExcludeJourneyService,
                                                   mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: TaxYear): Action[AnyContent] = auditProvider.unauthorisedPaymentsViewAuditing(taxYear.endYear) async { implicit request =>
    val cyaData    = request.sessionData
    val taxYearInt = taxYear.endYear

    if (cyaData.pensions.unauthorisedPayments.isFinished) {
      Future.successful(Ok(view(taxYearInt, cyaData.pensions.unauthorisedPayments)))
    } else {
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYearInt)
      redirectBasedOnCurrentAnswers(taxYearInt, Some(cyaData), cyaPageCall(taxYearInt))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYearInt, data.pensions.unauthorisedPayments)))
      }
    }
  }

  // TODO: We don't know what it does, but we're waiting for Business to create proper story about Exclusion (leaving for now)
  private def maybeExcludePension(unauthorisedPaymentModel: UnauthorisedPaymentsViewModel,
                                  taxYear: Int,
                                  priorAndSessionRequest: UserPriorAndSessionDataRequest[AnyContent])(implicit
      request: Request[_]): ApiResultT[Unit] =
    if (!unauthorisedPaymentModel.surchargeQuestion.exists(x => x) && !unauthorisedPaymentModel.noSurchargeQuestion.exists(x => x)) {
      EitherT(excludeJourneyService.excludeJourney("pensions", taxYear, priorAndSessionRequest.user.nino)(priorAndSessionRequest.user, hc)).void
    } else {
      EitherT.rightT[Future, APIErrorModel](())
    }

  def submit(taxYear: TaxYear): Action[AnyContent] = auditProvider.unauthorisedPaymentsUpdateAuditing(taxYear.endYear) async { implicit request =>
    val res = for {
      _ <- maybeExcludePension(request.sessionData.pensions.unauthorisedPayments, taxYear.endYear, request)
      result <- pensionsService.upsertUnauthorisedPaymentsFromPensions(
        request.user,
        taxYear,
        request.sessionData
      )(request.user.withDownstreamHc(hc), ec)
    } yield result

    handleResult(errorHandler, taxYear, Journey.UnauthorisedPayments, res)
  }

}
