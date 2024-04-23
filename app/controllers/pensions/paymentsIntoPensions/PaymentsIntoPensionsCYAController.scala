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

import cats.data.EitherT
import cats.implicits._
import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.predicates.auditActions.AuditActionsProvider
import models.redirects.AppLocations.SECTION_COMPLETED_PAGE
import models.pension.Journey.PaymentsIntoPensions
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.requests.UserPriorAndSessionDataRequest
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.PaymentsIntoPensionPages.CheckYourAnswersPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{ExcludeJourneyService, PensionReliefsService, PensionSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EqualsHelper.isDifferent
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                   view: PaymentsIntoPensionsCYAView,
                                                   pensionSessionService: PensionSessionService,
                                                   pensionReliefsService: PensionReliefsService,
                                                   errorHandler: ErrorHandler,
                                                   excludeJourneyService: ExcludeJourneyService,
                                                   mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  lazy val logger: Logger                         = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext

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

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.paymentsIntoPensionsUpdateAuditing(taxYear) async { implicit request =>
    val modelFromSession   = request.sessionData.pensions.paymentsIntoPension
    val modelFromPriorData = request.maybePrior.map(_.getPaymentsIntoPensionsCyaFromPrior)

    if (isDifferent(modelFromSession, modelFromPriorData)) {
      performSubmission(TaxYear(taxYear), modelFromSession)(hc, request)
    } else Future.successful(Redirect(SECTION_COMPLETED_PAGE(taxYear, PaymentsIntoPensions)))
  }

  private def excludeJourney(taxYear: TaxYear, paymentsIntoPensionFromSession: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      request: UserPriorAndSessionDataRequest[AnyContent]): EitherT[Future, Result, Unit] = {
    val user = request.user
    val res =
      if (!paymentsIntoPensionFromSession.rasPensionPaymentQuestion.exists(x =>
          x) && !paymentsIntoPensionFromSession.pensionTaxReliefNotClaimedQuestion
          .exists(x => x)) {
        // TODO: check conditions for excluding Pensions from submission without gateway
        excludeJourneyService.excludeJourney("pensions", taxYear.endYear, user.nino)(user, hc).map(_ => ())
      } else {
        Future.successful(())
      }

    EitherT
      .right[Result](res)
      .leftMap(_ => errorHandler.internalServerError())
  }

  private def performSubmission(taxYear: TaxYear, sessionData: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      request: UserPriorAndSessionDataRequest[AnyContent]): Future[Result] =
    (for {
      _      <- excludeJourney(taxYear, sessionData)
      result <- persist(taxYear, sessionData)
    } yield result).merge

  private def persist(taxYear: TaxYear, sessionData: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      request: UserPriorAndSessionDataRequest[AnyContent]): EitherT[Future, Result, Result] = {
    val res = pensionReliefsService.persistPaymentIntoPensionViewModel(
      request.user,
      taxYear,
      sessionData,
      request.maybePrior.flatMap(_.pensionReliefs.flatMap(_.pensionReliefs.overseasPensionSchemeContributions)))

    res.map(_ => Redirect(SECTION_COMPLETED_PAGE(taxYear.endYear, PaymentsIntoPensions))).leftMap { err =>
      logger.info(s"[PaymentIntoPensionsCYAController][submit] Failed to create or update session: ${err}")
      errorHandler.handleError(err.status)
    }
  }
}
