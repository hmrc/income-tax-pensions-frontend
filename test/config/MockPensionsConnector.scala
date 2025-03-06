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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
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

  def mockSavePaymentsIntoPensions(nino: Nino, taxYear: TaxYear): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.savePaymentsIntoPensions(eqTo(nino), eqTo(taxYear), any[PaymentsIntoPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveUkPensionIncome(nino: Nino, taxYear: TaxYear): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveUkPensionIncome(eqTo(nino), eqTo(taxYear), any[IncomeFromPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveAnnualAllowances(nino: Nino, taxYear: TaxYear): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveAnnualAllowances(eqTo(nino), eqTo(taxYear), any[PensionAnnualAllowancesViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveTransfersIntoOverseasPensions(nino: Nino, taxYear: TaxYear): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveTransfersIntoOverseasPensions(eqTo(nino), eqTo(taxYear), any[TransfersIntoOverseasPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

  def mockSaveIncomeFromOverseasPensions(nino: Nino, taxYear: TaxYear): ScalaOngoingStubbing[ApiResultT[Unit]] = {
    when(mockPensionsConnector.saveIncomeFromOverseasPensions(eqTo(nino), eqTo(taxYear), any[IncomeFromOverseasPensionsViewModel])
    (any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

  }

//  def mockGetAllJourneyStatuses(taxYear: TaxYear,
//                                response: Either[APIErrorModel,
//                                  List[JourneyNameAndStatus]]
//                               ): ScalaOngoingStubbing[DownstreamOutcome[List[JourneyNameAndStatus]]] = {
//    when(mockPensionsConnector.getAllJourneyStatuses(eqTo(taxYear))(any(), any()))
//      .thenReturn(Future.successful(response))
//
//  }

}
