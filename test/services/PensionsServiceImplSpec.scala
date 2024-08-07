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

import builders.PensionsUserDataBuilder.{aPensionsUserData, user}
import config.MockPensionsConnector
import models.pension.Journey
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import support.mocks.MockPensionSessionService
import utils.CommonData._

class PensionsServiceImplSpec extends AnyWordSpecLike with MockPensionsConnector with MockPensionSessionService {
  val service = new PensionsServiceImpl(mockPensionsConnector, mockPensionSessionService)
  val session = aPensionsUserData

  "upsertPaymentsIntoPensions" should {
    "remove answers after submission" in {
      mockSavePaymentsIntoPensions()
      mockClearSessionOnSuccess(Journey.PaymentsIntoPensions)
      val result = service.upsertPaymentsIntoPensions(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertUkIncomePension" should {
    "remove answers after submission" in {
      mockSaveUkPensionIncome()
      mockClearSessionOnSuccess(Journey.UkPensionIncome)
      val result = service.upsertUkPensionIncome(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertAnnualAllowances" should {
    "remove answers after submission" in {
      mockSaveAnnualAllowances()
      mockClearSessionOnSuccess(Journey.AnnualAllowances)
      val result = service.upsertAnnualAllowances(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertTransferIntoOverseasPensions" should {
    "remove answers after submission" in {
      val session = aPensionsUserData
      mockSaveTransfersIntoOverseasPensions()
      mockClearSessionOnSuccess(Journey.TransferIntoOverseasPensions)
      val result = service.upsertTransferIntoOverseasPensions(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertIncomeFromOverseasPensions" should {
    "remove answers after submission" in {
      mockSaveIncomeFromOverseasPensions()
      mockClearSessionOnSuccess(Journey.IncomeFromOverseasPensions)
      val result = service.upsertIncomeFromOverseasPensions(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertShortServiceRefunds" should {
    "remove answers after submission" in {
      mockShortServiceRefunds()
      mockClearSessionOnSuccess(Journey.ShortServiceRefunds)
      val result = service.upsertShortServiceRefunds(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }
}
