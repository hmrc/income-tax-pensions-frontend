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

package config

import cats.data.EitherT
import common.{Nino, TaxYear}
import connectors.PensionsConnector
import models.APIErrorModel
import models.mongo.{JourneyContext, JourneyStatus}
import models.pension.JourneyNameAndStatus
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionAnnualAllowancesViewModel, ShortServiceRefundsViewModel, TransfersIntoOverseasPensionsViewModel}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockPensionsConnector extends MockitoSugar {

  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]

  def mockDeletePensionChargesSessionData(
                                           nino: String,
                                           taxYear: Int,
                                           response: Either[APIErrorModel, Unit]
                                         ): Unit = {
    when(mockPensionsConnector.deletePensionCharges(
      eqTo(nino),
      eqTo(taxYear))
    (
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(Future.successful(response))
  }

  def mockGetAllJourneyStatuses(
                                 taxYear: TaxYear,
                                 response: Either[APIErrorModel, List[JourneyNameAndStatus]]
                               ): Unit = {
    when(mockPensionsConnector.getAllJourneyStatuses(
      eqTo(taxYear))
    (
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(Future.successful(response))
  }

  def mockGetJourneyStatus(
                            ctx: JourneyContext,
                            response: Either[APIErrorModel, List[JourneyNameAndStatus]]
                          ): Unit = {
    when(mockPensionsConnector.getJourneyStatus(
      eqTo(ctx))(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(Future.successful(response))
  }

  def mockSaveJourneyStatus(
                             ctx: JourneyContext,
                             status: JourneyStatus,
                             response: Either[APIErrorModel, Unit]
                           ): Unit = {
    when(mockPensionsConnector.saveJourneyStatus(
      eqTo(ctx),
      eqTo(status))(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(Future.successful(response))
  }

  def mockSavePaymentsIntoPensions(): Unit = {
    when(mockPensionsConnector.savePaymentsIntoPensions(
      any[Nino],
      any[TaxYear],
      any[PaymentsIntoPensionsViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }

  def mockSaveUkPensionIncome(): Unit = {
    when(mockPensionsConnector.saveUkPensionIncome(
      any[Nino],
      any[TaxYear],
      any[IncomeFromPensionsViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }

  def mockSaveAnnualAllowances(): Unit = {
    when(mockPensionsConnector.saveAnnualAllowances(
      any[Nino],
      any[TaxYear],
      any[PensionAnnualAllowancesViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }

  def mockSaveTransfersIntoOverseasPensions(): Unit = {
    when(mockPensionsConnector.saveTransfersIntoOverseasPensions(
      any[Nino],
      any[TaxYear],
      any[TransfersIntoOverseasPensionsViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }

  def mockSaveIncomeFromOverseasPensions(): Unit = {
    when(mockPensionsConnector.saveIncomeFromOverseasPensions(
      any[Nino],
      any[TaxYear],
      any[IncomeFromOverseasPensionsViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }

  def mockShortServiceRefunds(): Unit = {
    when(mockPensionsConnector.saveShortServiceRefunds(
      any[Nino],
      any[TaxYear],
      any[ShortServiceRefundsViewModel])(
      any[HeaderCarrier],
      any[ExecutionContext]
    )).thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  }
}