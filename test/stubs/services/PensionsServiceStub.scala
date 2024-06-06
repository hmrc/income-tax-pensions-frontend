/*
 * Copyright 2024 HM Revenue & Customs
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

package stubs.services

import cats.data.EitherT
import common.TaxYear
import connectors.ServiceError
import models.User
import models.domain.ApiResultT
import models.mongo.PensionsUserData
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import services.PensionsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

final case class PensionsServiceStub(upsertPaymentsIntoPensionsResult: Either[ServiceError, Unit] = Right(()),
                                     var paymentsIntoPensionsList: List[PaymentsIntoPensionsViewModel] = Nil,
                                     upsertUkPensionIncomeResult: Either[ServiceError, Unit] = Right(()),
                                     var incomeFromPensionsList: List[IncomeFromPensionsViewModel] = Nil,
                                     upsertAnnualAllowancesResult: Either[ServiceError, Unit] = Right(()),
                                     var annualAllowancesList: List[PensionAnnualAllowancesViewModel] = Nil,
                                     upsertTransfersIntoOverseasPensionsResult: Either[ServiceError, Unit] = Right(()),
                                     var transfersIntoOverseasPensionsList: List[TransfersIntoOverseasPensionsViewModel] = Nil,
                                     upsertIncomeFromOverseasPensionsResult: Either[ServiceError, Unit] = Right(()),
                                     var incomeFromOverseasPensionsList: List[IncomeFromOverseasPensionsViewModel] = Nil,
                                     upsertPaymentsIntoOverseasPensionsResult: Either[ServiceError, Unit] = Right(()),
                                     var paymentsIntoOverseasPensionsList: List[PaymentsIntoOverseasPensionsViewModel] = Nil,
                                     shortServiceRefundsResult: Either[ServiceError, Unit] = Right(()),
                                     var shortServiceRefundsList: List[ShortServiceRefundsViewModel] = Nil)
    extends PensionsService {

  def upsertPaymentsIntoPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    paymentsIntoPensionsList ::= sessionData.pensions.paymentsIntoPension
    EitherT.fromEither(upsertPaymentsIntoPensionsResult)
  }

  def upsertUkPensionIncome(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    incomeFromPensionsList ::= sessionData.pensions.incomeFromPensions
    EitherT.fromEither(upsertUkPensionIncomeResult)
  }

  def upsertAnnualAllowances(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    annualAllowancesList ::= sessionData.pensions.pensionsAnnualAllowances
    EitherT.fromEither(upsertAnnualAllowancesResult)
  }

  def upsertUnauthorisedPaymentsFromPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = ???

  def upsertTransferIntoOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    transfersIntoOverseasPensionsList ::= sessionData.pensions.transfersIntoOverseasPensions
    EitherT.fromEither(upsertTransfersIntoOverseasPensionsResult)
  }

  def upsertPaymentsIntoOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    paymentsIntoOverseasPensionsList ::= sessionData.pensions.paymentsIntoOverseasPensions
    EitherT.fromEither(upsertPaymentsIntoOverseasPensionsResult)
  }

  def upsertIncomeFromOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    incomeFromOverseasPensionsList ::= sessionData.pensions.incomeFromOverseasPensions
    EitherT.fromEither(upsertIncomeFromOverseasPensionsResult)
  }

  def upsertStatePension(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = ???

  def upsertShortServiceRefunds(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    shortServiceRefundsList ::= sessionData.pensions.shortServiceRefunds
    EitherT.fromEither(shortServiceRefundsResult)
  }
}
