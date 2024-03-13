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
import controllers.pensions.routes
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateSessionModelFromPrior
import models.pension.charges.UnauthorisedPaymentsViewModel
import models.requests.UserRequestWithSessionAndPrior
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.CYAPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import services.{ExcludeJourneyService, PensionSessionService, UnauthorisedPaymentsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsCYAController @Inject() (
    auditProvider: AuditActionsProvider,
    view: UnauthorisedPaymentsCYAView,
    pensionSessionService: PensionSessionService,
    service: UnauthorisedPaymentsService,
    errorHandler: ErrorHandler,
    excludeJourneyService: ExcludeJourneyService,
    mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = auditProvider.unauthorisedPaymentsViewAuditing(taxYear) async { implicit request =>
    val cyaData = request.sessionData
    if (!cyaData.pensions.unauthorisedPayments.isFinished) {
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) { data =>
        Future.successful(Ok(view(taxYear, data.pensions.unauthorisedPayments)))
      }
    } else {
      pensionSessionService.createOrUpdateSessionData(request.user, cyaData.pensions, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(Ok(view(taxYear, cyaData.pensions.unauthorisedPayments)))
    }
  }

  private def toSummaryRedirect(taxYear: Int) = Redirect(routes.PensionsSummaryController.show(taxYear))

  // TODO: check conditions for excluding Pensions from submission without gateway
  private def maybeExcludePension(unauthorisedPaymentModel: UnauthorisedPaymentsViewModel,
                                  taxYear: Int,
                                  priorAndSessionRequest: UserRequestWithSessionAndPrior[AnyContent])(implicit
      request: Request[_]): EitherT[Future, Result, Unit] =
    (if (!unauthorisedPaymentModel.surchargeQuestion.exists(x => x) && !unauthorisedPaymentModel.noSurchargeQuestion.exists(x => x)) {
       EitherT(excludeJourneyService.excludeJourney("pensions", taxYear, priorAndSessionRequest.user.nino)(priorAndSessionRequest.user, hc)).void
     } else {
       EitherT.rightT[Future, APIErrorModel](())
     }).leftSemiflatMap(_ => errorHandler.futureInternalServerError())

  private def maybeUpdateAnswers(cya: PensionsUserData, prior: Option[AllPensionsData], taxYear: Int)(implicit
      priorAndSessionRequest: UserRequestWithSessionAndPrior[AnyContent]): EitherT[Future, Result, Result] =
    if (isEqual(cya.pensions, prior)) {
      EitherT.rightT[Future, Result](toSummaryRedirect(taxYear))
    } else {
      EitherT.right[Result](performSubmission(taxYear, Some(cya))(priorAndSessionRequest.user, hc, priorAndSessionRequest))
    }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.unauthorisedPaymentsUpdateAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val (sessionData, priorData, unauthorisedPaymentModel) = (data, request.maybePrior, data.pensions.unauthorisedPayments)

      (for {
        _      <- maybeExcludePension(unauthorisedPaymentModel, taxYear, request).map(_ => toSummaryRedirect(taxYear))
        result <- maybeUpdateAnswers(sessionData, priorData, taxYear)
      } yield result).merge
    }
  }

  private def performSubmission(taxYear: Int, cya: Option[PensionsUserData])(implicit
      user: User,
      hc: HeaderCarrier,
      request: UserRequestWithSessionAndPrior[AnyContent]): Future[Result] =
    (cya match {
      case Some(_) =>
        service.saveAnswers(user, TaxYear(taxYear)) map {
          case Left(_) =>
            logger.info("[submit] Failed to create or update session")
            Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel(BAD_REQUEST.toString, "Unable to createOrUpdate pension service")))
          case Right(_) =>
            Right(Ok)
        }
      case _ =>
        logger.info("[submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        Future.successful(Redirect(routes.PensionsSummaryController.show(taxYear)))
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }

  private def isEqual(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean =
    priorData match {
      case None        => false
      case Some(prior) => cyaData.equals(generateSessionModelFromPrior(prior))
    }

}
