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
import cats.implicits.{catsSyntaxApplicativeId, toBifunctorOps}
import config.ErrorHandler
import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.AllPensionsData
import models.session.PensionCYAMergedWithPriorData
import models.{APIErrorModel, User}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{Request, Result}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock
import utils.EitherTUtils.EitherTOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSessionService @Inject() (sessionRepository: PensionsUserDataRepository,
                                       incomeTaxConnector: IncomeTaxUserDataConnector,
                                       errorHandler: ErrorHandler)(implicit ec: ExecutionContext)
    extends Logging {

  def loadPriorData(taxYear: Int, user: User)(implicit hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] =
    incomeTaxConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))

  def loadSessionData(taxYear: Int, user: User): Future[Either[Unit, Option[PensionsUserData]]] =
    sessionRepository
      .find(taxYear, user)
      .map(_.leftMap(_ => ()))

  def getPensionsSessionDataResult(taxYear: Int, user: User)(result: Option[PensionsUserData] => Future[Result])(implicit
      request: Request[_]): Future[Result] =
    sessionRepository.find(taxYear, user).flatMap {
      case Left(_)      => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }

  def loadDataAndHandle(taxYear: Int, user: User)(block: (Option[PensionsUserData], Option[AllPensionsData]) => Future[Result])(implicit
      request: Request[_],
      hc: HeaderCarrier): Future[Result] = {
    val resultT = for {
      maybeSession <- EitherT(sessionRepository.find(taxYear, user)).leftAs[ServiceError]
      prior        <- EitherT(loadPriorData(taxYear, user)).leftAs[ServiceError]
    } yield block(maybeSession, prior.pensions)

    resultT.value.flatMap {
      case Left(error: APIErrorModel) => errorHandler.handleError(error.status).pure[Future]
      case Left(_)                    => errorHandler.handleError(INTERNAL_SERVER_ERROR).pure[Future]
      case Right(value)               => value
    }
  }

  // scalastyle:off
  def createOrUpdateSessionData[A](user: User, cyaModel: PensionsCYAModel, taxYear: Int, isPriorSubmission: Boolean)(onFail: A)(onSuccess: A)(implicit
      clock: Clock): Future[A] = {

    val userData = PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    sessionRepository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_)  => onFail
    }
  }

  def createOrUpdateSessionData(pensionsUserData: PensionsUserData): Future[Either[DatabaseError, Unit]] =
    sessionRepository.createOrUpdate(pensionsUserData).map {
      case Left(error: DatabaseError) => Left(error)
      case Right(_)                   => Right(())
    }

  def mergePriorDataToSession(
      taxYear: Int,
      user: User,
      renderView: (Int, PensionsCYAModel, Option[AllPensionsData]) => Result
  )(implicit request: Request[_], hc: HeaderCarrier, clock: Clock): Future[Result] =
    loadDataAndHandle(taxYear, user) { (sessionData, priorData) =>
      createOrUpdateSessionIfNeeded(sessionData, priorData, taxYear, user, renderView)
    }

  private def createOrUpdateSessionIfNeeded(
      sessionData: Option[PensionsUserData],
      priorData: Option[AllPensionsData],
      taxYear: Int,
      user: User,
      renderView: (Int, PensionsCYAModel, Option[AllPensionsData]) => Result
  )(implicit request: Request[_], clock: Clock) = {
    val updatedSession                = PensionCYAMergedWithPriorData.mergeSessionAndPriorData(sessionData, priorData)
    val updatedSessionPensionCYAModel = updatedSession.newPensionsCYAModel
    val summaryView                   = renderView(taxYear, updatedSessionPensionCYAModel, priorData)

    if (updatedSession.newModelChanged) {
      createOrUpdateSessionData(user, updatedSessionPensionCYAModel, taxYear, isPriorSubmission = priorData.isDefined)(
        errorHandler.handleError(INTERNAL_SERVER_ERROR))(summaryView)
    } else {
      Future.successful(summaryView)
    }
  }
}
