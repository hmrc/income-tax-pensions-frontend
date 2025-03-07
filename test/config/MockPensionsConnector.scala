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

package config

import cats.data.EitherT
import common.{Nino, TaxYear}
import connectors.{DownstreamOutcome, PensionsConnector}
import models.APIErrorModel
import models.domain.ApiResultT
import models.mongo.{JourneyContext, JourneyStatus}
import models.pension.JourneyNameAndStatus
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockPensionsConnector extends MockitoSugar {


  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]

  def mockGetAllJourneyStatuses(taxYear: TaxYear,
                                response: Either[APIErrorModel,
                                  List[JourneyNameAndStatus]]
                               ): ScalaOngoingStubbing[DownstreamOutcome[List[JourneyNameAndStatus]]] = {

    when(mockPensionsConnector.getAllJourneyStatuses(eqTo(taxYear))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(response))

  }

  def mockGetJourneyStatus(ctx: JourneyContext,
                           response: Either[APIErrorModel,
                             List[JourneyNameAndStatus]]
                          ): ScalaOngoingStubbing[DownstreamOutcome[List[JourneyNameAndStatus]]] = {
    when(mockPensionsConnector.getJourneyStatus(eqTo(ctx))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(response))

  }

  def mockSaveJourneyStatus(
      ctx: JourneyContext,
      status: JourneyStatus,
      response: Either[APIErrorModel, Unit]): ScalaOngoingStubbing[DownstreamOutcome[Unit]] = {
    when(mockPensionsConnector.saveJourneyStatus(eqTo(ctx), eqTo(status))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(response))

  }

  def mockSavePaymentsIntoPensions(): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.savePaymentsIntoPensions(any[Nino], any[TaxYear], any[PaymentsIntoPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveUkPensionIncome(): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveUkPensionIncome(any[Nino], any[TaxYear], any[IncomeFromPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveAnnualAllowances(): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveAnnualAllowances(any[Nino], any[TaxYear], any[PensionAnnualAllowancesViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveTransfersIntoOverseasPensions(): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveTransfersIntoOverseasPensions(any[Nino], any[TaxYear], any[TransfersIntoOverseasPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveIncomeFromOverseasPensions(): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveIncomeFromOverseasPensions(any[Nino], any[TaxYear], any[IncomeFromOverseasPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockShortServiceRefunds(): ScalaOngoingStubbing[ApiResultT[Unit]] =
    when(mockPensionsConnector.saveShortServiceRefunds(any[Nino], any[TaxYear], any[ShortServiceRefundsViewModel])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

}
