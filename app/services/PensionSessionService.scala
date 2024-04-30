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

package services

import cats.data.EitherT
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, toBifunctorOps}
import common.TaxYear
import config.ErrorHandler
import connectors.{DownstreamOutcome, IncomeTaxUserDataConnector, PensionsConnector}
import models.IncomeTaxUserData.PriorData
import models.domain.ApiResultT
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.PensionsUserData.SessionData
import models.mongo._
import models.pension.AllPensionsData.PriorPensionsData
import models.pension.Journey._
import models.pension.{Journey, JourneyNameAndStatus}
import models.redirects.AppLocations.SECTION_COMPLETED_PAGE
import models.session.PensionCYAMergedWithPriorData
import models.{APIErrorModel, User}
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResult
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps
import viewmodels.JourneyStatusSummaryViewModel.buildSummaryList

import java.time.{Clock, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSessionService @Inject() (repository: PensionsUserDataRepository,
                                       submissionsConnector: IncomeTaxUserDataConnector,
                                       pensionsConnector: PensionsConnector,
                                       errorHandler: ErrorHandler)(implicit ec: ExecutionContext)
    extends Logging {

  def loadSessionData(taxYear: Int, user: User): Future[Either[Unit, Option[SessionData]]] =
    repository.find(taxYear, user).map(_.leftMap(_ => ()))

  /** It loads only provided journey (if exist in the Backend). All other Journeys are empty.
    */
  def loadOneJourneyPriorData(taxYear: TaxYear, user: User, journey: Journey)(implicit hc: HeaderCarrier): ApiResultT[PensionsCYAModel] =
    journey match {
      case PaymentsIntoPensions =>
        pensionsConnector
          .getPaymentsIntoPensions(user.getNino, taxYear)
          .map(_.map(_.toPensionsCYAModel).getOrElse(PensionsCYAModel.emptyModels))
      case _ => ??? // TODO We'll be adding gradually journey by journey here
    }

  def loadPriorAndSession(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcomeT[(PriorData, SessionData)] =
    for {
      prior        <- EitherT(loadPriorData(taxYear.endYear, user)).leftAs[ServiceError]
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
    } yield (prior, session)

  def loadPriorData(taxYear: Int, user: User)(implicit hc: HeaderCarrier): DownstreamOutcome[PriorData] =
    submissionsConnector.getUserData(user.nino, taxYear)(hc.withMtditId(user.mtditid))

  def getPensionsSessionDataResult(taxYear: Int, user: User)(result: Option[SessionData] => Future[Result])(implicit
      request: Request[_]): Future[Result] =
    repository.find(taxYear, user).flatMap {
      case Left(_)      => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }

  def upsertSession(session: PensionsUserData)(implicit ec: ExecutionContext, request: Request[_]): EitherT[Future, Result, Unit] =
    upsertSessionT(session).leftMap(_ => errorHandler.internalServerError())

  def upsertSessionT(session: PensionsUserData)(implicit ec: ExecutionContext): EitherT[Future, ServiceError, Unit] =
    EitherT(repository.createOrUpdate(session))

  def createOrUpdateSession(pensionsUserData: SessionData): QueryResult[Unit] =
    repository.createOrUpdate(pensionsUserData).map {
      case Left(error: DatabaseError) => Left(error)
      case Right(_)                   => Right(())
    }

  def mergePriorDataToSession(summaryPage: Journey, taxYear: Int, user: User, renderView: (Int, HtmlContent) => Result)(implicit
      request: Request[_],
      messages: Messages,
      hc: HeaderCarrier): Future[Result] =
    loadDataAndHandle(taxYear, user) { (sessionData, priorData, journeyStatuses) =>
      createOrUpdateSessionIfNeeded(summaryPage, sessionData, priorData, taxYear, user, journeyStatuses, renderView)
    }

  def loadDataAndHandle(taxYear: Int, user: User)(
      block: (Option[SessionData], Option[PriorPensionsData], Seq[JourneyNameAndStatus]) => Future[Result])(implicit
      request: Request[_],
      hc: HeaderCarrier): Future[Result] = {
    val resultT = for {
      maybeSession <- EitherT(repository.find(taxYear, user)).leftAs[ServiceError]
      prior        <- EitherT(loadPriorData(taxYear, user)).leftAs[ServiceError]
      journeyStatuses <- EitherT(pensionsConnector.getAllJourneyStatuses(TaxYear(taxYear))(hc.withMtditId(user.mtditid), ec))
        .leftAs[ServiceError]
    } yield block(maybeSession, prior.pensions, journeyStatuses)

    resultT.value.flatMap {
      case Left(error: APIErrorModel) => errorHandler.handleError(error.status).pure[Future]
      case Left(_)                    => errorHandler.handleError(INTERNAL_SERVER_ERROR).pure[Future]
      case Right(value)               => value
    }
  }

  private def createOrUpdateSessionIfNeeded(
      summaryPage: Journey,
      sessionData: Option[SessionData],
      priorData: Option[PriorPensionsData],
      taxYear: Int,
      user: User,
      journeyStatuses: Seq[JourneyNameAndStatus],
      renderView: (Int, HtmlContent) => Result)(implicit request: Request[_], messages: Messages): Future[Result] = {
    val updatedSession                = PensionCYAMergedWithPriorData.mergeSessionAndPriorData(sessionData, priorData)
    val updatedSessionPensionCYAModel = updatedSession.newPensionsCYAModel
    val pensionsSummary               = buildSummaryList(summaryPage, journeyStatuses, priorData, updatedSessionPensionCYAModel.some, taxYear)
    val summaryView                   = renderView(taxYear, pensionsSummary)

    if (updatedSession.newModelChanged) {
      createOrUpdateSessionData(user, updatedSessionPensionCYAModel, taxYear, isPriorSubmission = priorData.isDefined)(
        errorHandler.handleError(INTERNAL_SERVER_ERROR))(summaryView)
    } else {
      Future.successful(summaryView)
    }
  }

  def createOrUpdateSessionData[A](user: User, cyaModel: PensionsCYAModel, taxYear: Int, isPriorSubmission: Boolean)(onFail: => A)(
      onSuccess: => A): Future[A] = {

    val userData = PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      cyaModel,
      Clock.systemUTC().instant().atZone(ZoneOffset.UTC)
    )

    repository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_)  => onFail
    }
  }

  def updateSessionData(user: User, taxYear: TaxYear, sessionData: PensionsUserData, priorData: PensionsCYAModel): ApiResultT[PensionsUserData] = {
    val updatedSessionData = priorData.merge(Some(sessionData.pensions))

    if (updatedSessionData == sessionData.pensions) {
      EitherT.rightT[Future, APIErrorModel](sessionData)
    } else {
      val userData = PensionsUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear.endYear,
        true,
        updatedSessionData,
        Clock.systemUTC().instant().atZone(ZoneOffset.UTC)
      )

      EitherT(repository.createOrUpdate(userData))
        .bimap(
          _.toAPIErrorModel,
          _ => userData
        )
    }
  }

  def clearSessionOnSuccess(journey: Journey, existingSessionData: PensionsUserData): ApiResultT[Unit] = {
    def updatedSession = existingSessionData.removeJourneyAnswers(journey)

    upsertSessionT(updatedSession).leftMap(_.toAPIErrorModel)
  }

}
