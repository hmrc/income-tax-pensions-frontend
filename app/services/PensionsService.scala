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

package services

import common.TaxYear
import connectors.PensionsConnector
import models.User
import models.domain.ApiResultT
import models.mongo.PensionsUserData
import models.pension.Journey
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait PensionsService {
  def upsertPaymentsIntoPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
  def upsertUkPensionIncome(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
  def upsertAnnualAllowances(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
  def upsertUnauthorisedPaymentsFromPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
  def upsertPaymentsIntoOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
  def upsertIncomeFromOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
}

@Singleton
class PensionsServiceImpl @Inject() (pensionsConnector: PensionsConnector, sessionService: PensionSessionService) extends PensionsService {
  def upsertPaymentsIntoPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.savePaymentsIntoPensions(user.getNino, taxYear, sessionData.pensions.paymentsIntoPension)
      _ <- sessionService.clearSessionOnSuccess(Journey.PaymentsIntoPensions, sessionData)
    } yield ()

  def upsertUkPensionIncome(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.saveUkPensionIncome(user.getNino, taxYear, sessionData.pensions.incomeFromPensions)
      _ <- sessionService.clearSessionOnSuccess(Journey.UkPensionIncome, sessionData)
    } yield ()

  def upsertAnnualAllowances(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.saveAnnualAllowances(user.getNino, taxYear, sessionData.pensions.pensionsAnnualAllowances)
      _ <- sessionService.clearSessionOnSuccess(Journey.AnnualAllowances, sessionData)
    } yield ()

  def upsertUnauthorisedPaymentsFromPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.saveUnauthorisedPaymentsFromPensions(user.getNino, taxYear, sessionData.pensions.unauthorisedPayments)
      _ <- sessionService.clearSessionOnSuccess(Journey.UnauthorisedPayments, sessionData)
    } yield ()

  def upsertPaymentsIntoOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.savePaymentsIntoOverseasPensions(user.getNino, taxYear, sessionData.pensions.paymentsIntoOverseasPensions)
      _ <- sessionService.clearSessionOnSuccess(Journey.PaymentsIntoOverseasPensions, sessionData)
    } yield ()

  def upsertIncomeFromOverseasPensions(user: User, taxYear: TaxYear, sessionData: PensionsUserData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    for {
      _ <- pensionsConnector.saveIncomeFromOverseasPensions(user.getNino, taxYear, sessionData.pensions.incomeFromOverseasPensions)
      _ <- sessionService.clearSessionOnSuccess(Journey.IncomeFromOverseasPensions, sessionData)
    } yield ()
}
