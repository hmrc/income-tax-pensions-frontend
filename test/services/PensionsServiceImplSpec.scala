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

package services


import builders.PensionsUserDataBuilder.{aPensionsUserData, user}
import common.Nino
import config.MockPensionsConnector
import models.mongo.PensionsUserData
import models.pension.Journey
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import support.mocks.MockPensionSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonData._

import scala.concurrent.ExecutionContext

class PensionsServiceImplSpec extends AnyWordSpecLike with MockPensionsConnector with MockPensionSessionService {

  val service: PensionsServiceImpl = new PensionsServiceImpl(mockPensionsConnector, mockPensionSessionService)
  val session: PensionsUserData = aPensionsUserData
  val nino: Nino = Nino("nino")
  implicit val ec: ExecutionContext = ExecutionContext.global

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  "upsertPaymentsIntoPensions" should {
    "remove answers after submission" in {
      mockSavePaymentsIntoPensions()
      mockClearSessionOnSuccess(Journey.PaymentsIntoPensions, session)

      val result = service.upsertPaymentsIntoPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertUkIncomePension" should {
    "remove answers after submission" in {
      mockSaveUkPensionIncome()
      mockClearSessionOnSuccess(Journey.UkPensionIncome, session)

      val result = service.upsertUkPensionIncome(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertAnnualAllowances" should {
    "remove answers after submission" in {
      mockSaveAnnualAllowances()
      mockClearSessionOnSuccess(Journey.AnnualAllowances, session)

      val result = service.upsertAnnualAllowances(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertTransferIntoOverseasPensions" should {
    "remove answers after submission" in {
      val session = aPensionsUserData

      mockSaveTransfersIntoOverseasPensions()
      mockClearSessionOnSuccess(Journey.TransferIntoOverseasPensions, session)

      val result = service.upsertTransferIntoOverseasPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertIncomeFromOverseasPensions" should {
    "remove answers after submission" in {
      mockSaveIncomeFromOverseasPensions()
      mockClearSessionOnSuccess(Journey.IncomeFromOverseasPensions, session)

      val result = service.upsertIncomeFromOverseasPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertShortServiceRefunds" should {
    "remove answers after submission" in {
      mockShortServiceRefunds()
      mockClearSessionOnSuccess(Journey.ShortServiceRefunds, session)

      val result = service.upsertShortServiceRefunds(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

}
