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

package support.mocks

import cats.data.EitherT
import connectors.{DownstreamErrorOr, DownstreamOutcome}
import models._
import models.mongo.{DatabaseError, PensionsUserData}
import models.pension.Journey
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}
import services.PensionSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockPensionSessionService extends MockFactory {

  val mockPensionSessionService: PensionSessionService = mock[PensionSessionService]

  def mockGetPriorData(taxYear: Int,
                       user: User,
                       result: DownstreamErrorOr[IncomeTaxUserData]): CallHandler3[Int, User, HeaderCarrier, DownstreamOutcome[IncomeTaxUserData]] =
    (mockPensionSessionService
      .loadPriorData(_: Int, _: User)(_: HeaderCarrier))
      .expects(taxYear, user, *)
      .returning(Future.successful(result))

  def mockGetPensionsSessionDataResult(
      taxYear: Int,
      result: Result): CallHandler4[Int, User, Option[PensionsUserData] => Future[Result], Request[_], Future[Result]] =
    (mockPensionSessionService
      .getPensionsSessionDataResult(_: Int, _: User)(_: Option[PensionsUserData] => Future[Result])(_: Request[_]))
      .expects(taxYear, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()

  def mockGetPensionSessionData(
      taxYear: Int,
      result: Either[Unit, Option[PensionsUserData]]): CallHandler2[Int, User, Future[Either[Unit, Option[PensionsUserData]]]] =
    (mockPensionSessionService
      .loadSessionData(_: Int, _: User))
      .expects(taxYear, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()

  def mockCreateOrUpdateSessionData(userData: PensionsUserData, result: Either[DatabaseError, Unit] = Right(())): Unit =
    (mockPensionSessionService
      .createOrUpdateSession(_: PensionsUserData))
      .expects(userData)
      .returns(Future.successful(result))
      .anyNumberOfTimes()

  def mockClearSessionOnSuccess(expectedJourney: Journey, result: Either[APIErrorModel, Unit] = Right()): Unit =
    (mockPensionSessionService
      .clearSessionOnSuccess(_: Journey, _: PensionsUserData))
      .expects(expectedJourney, *)
      .returns(EitherT.fromEither[Future](result))
      .anyNumberOfTimes()
}
