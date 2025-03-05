/*
 * Copyright 2025 HM Revenue & Customs
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
import models.IncomeTaxUserData.PriorData
import models._
import models.mongo.{DatabaseError, PensionsUserData}
import models.pension.Journey
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait MockPensionSessionService extends MockitoSugar {

  val mockPensionSessionService: PensionSessionService = mock[PensionSessionService]

  implicit val hc: HeaderCarrier = HeaderCarrier()


  def mockGetPriorData(taxYear: Int,
                       user: User,
                       result: DownstreamErrorOr[IncomeTaxUserData]): OngoingStubbing[DownstreamOutcome[PriorData]] = {

    when(mockPensionSessionService.loadPriorData(eqTo(taxYear), eqTo(user)))
      .thenReturn(Future.successful(result))

    //    (mockPensionSessionService
    //      .loadPriorData(_: Int, _: User)(_: HeaderCarrier))
    //      .expects(taxYear, user, *)
    //      .returning(Future.successful(result)))
  }

  def mockGetPensionsSessionDataResult(
                                        taxYear: Int,
                                        user: User,
                                        result: Result): OngoingStubbing[Future[Result]] = {

    when(mockPensionSessionService.getPensionsSessionDataResult(eqTo(taxYear), eqTo(user))(any[Option[PensionsUserData] => Future[Result]])(any()))
      .thenReturn(Future.successful(result))

    //    (mockPensionSessionService
    //      .getPensionsSessionDataResult(_: Int, _: User)(_: Option[PensionsUserData] => Future[Result])(_: Request[_]))
    //      .expects(taxYear, *, *, *)
    //      .returns(Future.successful(result))
    //      .anyNumberOfTimes()

  }

  def mockGetPensionSessionData(
                                 taxYear: Int,
                                 user: User,
                                 result: Either[Unit, Option[PensionsUserData]]
                               ): OngoingStubbing[Future[Either[Unit, Option[PensionsUserData.SessionData]]]] = {
    //    (mockPensionSessionService
    //      .loadSessionData(_: Int, _: User))
    //      .expects(taxYear, *)
    //      .returns(Future.successful(result))
    //      .anyNumberOfTimes()

    when(mockPensionSessionService.loadSessionData(eqTo(taxYear), eqTo(user)))
      .thenReturn(Future.successful(result))
  }

  def mockCreateOrUpdateSessionData(userData: PensionsUserData, result: Either[DatabaseError, Unit] = Right(())): Unit = {

    when(mockPensionSessionService.createOrUpdateSession(eqTo(userData)))
      .thenReturn(Future.successful(result))

//    (mockPensionSessionService
//      .createOrUpdateSession(_: PensionsUserData))
//      .expects(userData)
//      .returns(Future.successful(result))
//      .anyNumberOfTimes()
  }

  def mockClearSessionOnSuccess(expectedJourney: Journey, pensionsUserData: PensionsUserData, result: Either[APIErrorModel, Unit] = Right(())): Unit = {

    when(mockPensionSessionService.clearSessionOnSuccess(eqTo(expectedJourney), eqTo(pensionsUserData)))
      .thenReturn(EitherT.fromEither[Future](result))
//
//    (mockPensionSessionService
//      .clearSessionOnSuccess(_: Journey, _: PensionsUserData))
//      .expects(expectedJourney, *)
//      .returns(EitherT.fromEither[Future](result))
//      .anyNumberOfTimes()
  }
}
