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
import connectors.DownstreamErrorOr
import models._
import models.mongo.{DatabaseError, PensionsUserData}
import models.pension.Journey
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Request, Result}
import services.PensionSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockPensionSessionService extends MockitoSugar {

  val mockPensionSessionService: PensionSessionService = mock[PensionSessionService]

  def mockGetPriorData(
                        taxYear: Int,
                        user: User,
                        result: DownstreamErrorOr[IncomeTaxUserData]
                      ): Unit = {
    when(mockPensionSessionService.loadPriorData(eqTo(taxYear), eqTo(user))(any[HeaderCarrier]))
      .thenReturn(Future.successful(result))
  }

  def mockGetPensionsSessionDataResult(
                                        taxYear: Int,
                                        result: Result
                                      ): Unit = {
    when(mockPensionSessionService.getPensionsSessionDataResult(
      eqTo(taxYear),
      any[User]
    )
    (any[Option[PensionsUserData] => Future[Result]])
    (any[Request[_]]
    )).thenReturn(Future.successful(result))
  }

  def mockGetPensionSessionData(
                                 taxYear: Int,
                                 result: Either[Unit, Option[PensionsUserData]]
                               ): Unit = {
    when(mockPensionSessionService.loadSessionData(eqTo(taxYear), any[User]))
      .thenReturn(Future.successful(result))
  }

  def mockCreateOrUpdateSessionData(
                                     userData: PensionsUserData,
                                     result: Either[DatabaseError, Unit] = Right(())
                                   ): Unit = {
    when(mockPensionSessionService.createOrUpdateSession(eqTo(userData)))
      .thenReturn(Future.successful(result))
  }

  def mockClearSessionOnSuccess(
                                 expectedJourney: Journey,
                                 result: Either[APIErrorModel, Unit] = Right(())
                               ): Unit = {
    when(mockPensionSessionService.clearSessionOnSuccess(eqTo(expectedJourney), any[PensionsUserData]))
      .thenReturn(EitherT.fromEither[Future](result))
  }
}