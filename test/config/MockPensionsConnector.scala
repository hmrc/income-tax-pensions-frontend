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
import connectors.{DownstreamOutcome, PensionsConnector}
import models.APIErrorModel
import models.domain.ApiResultT
import models.mongo.{JourneyContext, JourneyStatus}
import models.pension.JourneyNameAndStatus
import models.pension.charges.{
  IncomeFromOverseasPensionsViewModel,
  PensionAnnualAllowancesViewModel,
  ShortServiceRefundsViewModel,
  TransfersIntoOverseasPensionsViewModel
}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.scalamock.handlers.{CallHandler3, CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockPensionsConnector extends MockFactory {

  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]

  def mockDeletePensionChargesSessionData(
      nino: String,
      taxYear: Int,
      response: Either[APIErrorModel, Unit]): CallHandler4[String, Int, HeaderCarrier, ExecutionContext, DownstreamOutcome[Unit]] =
    (mockPensionsConnector
      .deletePensionCharges(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockGetAllJourneyStatuses(taxYear: TaxYear, response: Either[APIErrorModel, List[JourneyNameAndStatus]])
      : CallHandler3[TaxYear, HeaderCarrier, ExecutionContext, DownstreamOutcome[List[JourneyNameAndStatus]]] =
    (mockPensionsConnector
      .getAllJourneyStatuses(_: TaxYear)(_: HeaderCarrier, _: ExecutionContext))
      .expects(taxYear, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockGetJourneyStatus(ctx: JourneyContext, response: Either[APIErrorModel, List[JourneyNameAndStatus]])
      : CallHandler3[JourneyContext, HeaderCarrier, ExecutionContext, DownstreamOutcome[List[JourneyNameAndStatus]]] =
    (mockPensionsConnector
      .getJourneyStatus(_: JourneyContext)(_: HeaderCarrier, _: ExecutionContext))
      .expects(ctx, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockSaveJourneyStatus(
      ctx: JourneyContext,
      status: JourneyStatus,
      response: Either[APIErrorModel, Unit]): CallHandler4[JourneyContext, JourneyStatus, HeaderCarrier, ExecutionContext, DownstreamOutcome[Unit]] =
    (mockPensionsConnector
      .saveJourneyStatus(_: JourneyContext, _: JourneyStatus)(_: HeaderCarrier, _: ExecutionContext))
      .expects(ctx, status, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockSavePaymentsIntoPensions(): CallHandler5[Nino, TaxYear, PaymentsIntoPensionsViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .savePaymentsIntoPensions(_: Nino, _: TaxYear, _: PaymentsIntoPensionsViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

  def mockSaveUkPensionIncome(): CallHandler5[Nino, TaxYear, IncomeFromPensionsViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .saveUkPensionIncome(_: Nino, _: TaxYear, _: IncomeFromPensionsViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

  def mockSaveAnnualAllowances(): CallHandler5[Nino, TaxYear, PensionAnnualAllowancesViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .saveAnnualAllowances(_: Nino, _: TaxYear, _: PensionAnnualAllowancesViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

  def mockSaveTransfersIntoOverseasPensions()
      : CallHandler5[Nino, TaxYear, TransfersIntoOverseasPensionsViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .saveTransfersIntoOverseasPensions(_: Nino, _: TaxYear, _: TransfersIntoOverseasPensionsViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

  def mockSaveIncomeFromOverseasPensions()
      : CallHandler5[Nino, TaxYear, IncomeFromOverseasPensionsViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .saveIncomeFromOverseasPensions(_: Nino, _: TaxYear, _: IncomeFromOverseasPensionsViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

  def mockShortServiceRefunds(): CallHandler5[Nino, TaxYear, ShortServiceRefundsViewModel, HeaderCarrier, ExecutionContext, ApiResultT[Unit]] =
    (mockPensionsConnector
      .saveShortServiceRefunds(_: Nino, _: TaxYear, _: ShortServiceRefundsViewModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(EitherT.rightT[Future, APIErrorModel](()))
      .anyNumberOfTimes()

}
