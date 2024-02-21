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

package models.pension.charges

import builders.UnauthorisedPaymentsViewModelBuilder._
import cats.implicits.catsSyntaxOptionId
import support.UnitTest

class UnauthorisedPaymentsViewModelSpec extends UnitTest {

  val amount: BigDecimal     = BigDecimal(123.00)
  val zeroAmount: BigDecimal = BigDecimal(0.00)

  "copyWithQuestionsApplied" should {
    "copy the content of the model without modifying" when {
      "the questions are set to true" in {
        completeViewModel.copyWithQuestionsApplied(Some(true), Some(true)) shouldBe completeViewModel
      }
    }

    "copy the model and set the values to None" when {
      "the 'surcharge' question is false" in {
        val expected = completeViewModel
          .copy(surchargeQuestion = Some(false), surchargeAmount = None, surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
        completeViewModel.copyWithQuestionsApplied(Some(false), Some(true)) shouldBe expected
      }

      "the 'no surcharge' question is false" in {
        val expected = completeViewModel
          .copy(noSurchargeQuestion = Some(false), noSurchargeAmount = None, noSurchargeTaxAmountQuestion = None, noSurchargeTaxAmount = None)
        completeViewModel.copyWithQuestionsApplied(Some(true), Some(false)) shouldBe expected
      }

      "both 'surcharge' and 'no surcharge' are false" in {
        val expected = completeViewModel
          .copy(
            surchargeQuestion = Some(false),
            surchargeAmount = None,
            surchargeTaxAmountQuestion = None,
            surchargeTaxAmount = None,
            noSurchargeQuestion = Some(false),
            noSurchargeAmount = None,
            noSurchargeTaxAmountQuestion = None,
            noSurchargeTaxAmount = None,
            ukPensionSchemesQuestion = None,
            pensionSchemeTaxReference = None
          )
        completeViewModel.copyWithQuestionsApplied(Some(false), Some(false)) shouldBe expected
      }
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        completeViewModel.isFinished
      }
      "all required questions are answered" in {
        completeViewModel
          .copy(
            surchargeQuestion = Some(false),
            surchargeAmount = None,
            surchargeTaxAmountQuestion = None,
            surchargeTaxAmount = None
          )
          .isFinished shouldBe true

        completeViewModel
          .copy(
            surchargeTaxAmountQuestion = Some(false),
            surchargeTaxAmount = None,
            noSurchargeQuestion = Some(false),
            noSurchargeAmount = None,
            noSurchargeTaxAmountQuestion = None,
            noSurchargeTaxAmount = None,
            ukPensionSchemesQuestion = Some(false),
            pensionSchemeTaxReference = None
          )
          .isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        completeViewModel
          .copy(
            surchargeQuestion = Some(true),
            surchargeAmount = None,
            surchargeTaxAmountQuestion = Some(false),
            surchargeTaxAmount = None
          )
          .isFinished
      }
    }
  }

  "toDownstreamRequestModel" when {
    "claims exist" should {
      "create the model with the claim amounts unchanged" in {
        val expectedResult =
          PensionSchemeUnauthorisedPayments(
            List("some_pstr").some,
            surcharge = Charge(amount, amount).some,
            noSurcharge = Charge(amount, amount).some
          )

        completeViewModel.toDownstreamRequestModel shouldBe expectedResult
      }
    }
    "neither claim exists" should {
      "send 0s downstream for both claims" in {
        val expectedResult =
          PensionSchemeUnauthorisedPayments(
            List("some_pstr").some,
            surcharge = Charge(zeroAmount, zeroAmount).some,
            noSurcharge = Charge(zeroAmount, zeroAmount).some
          )

        neitherClaimViewModel.toDownstreamRequestModel shouldBe expectedResult
      }
    }
    "only one of the claims exists" should {
      "treat them independently, not changing claimed values and sending 0s for the unclaimed" in {
        val expectedResult =
          PensionSchemeUnauthorisedPayments(
            List("some_pstr").some,
            surcharge = Charge(amount, amount).some,
            noSurcharge = Charge(zeroAmount, zeroAmount).some
          )

        surchargeOnlyViewModel.toDownstreamRequestModel shouldBe expectedResult
      }
    }
  }

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      anUnauthorisedPaymentsEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      anUnauthorisedPaymentsViewModel.isEmpty shouldBe false
      anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = None).isEmpty shouldBe false
      anUnauthorisedPaymentsEmptyViewModel.copy(surchargeQuestion = Some(false)).isEmpty shouldBe false
    }
  }

  "unauthorisedPaymentQuestion" should {
    "return 'Some(true)' when surchargeQuestion and/or noSurchargeQuestion are 'true'" in {
      anUnauthorisedPaymentsEmptyViewModel
        .copy(surchargeQuestion = Some(true), noSurchargeQuestion = Some(false))
        .unauthorisedPaymentQuestion shouldBe Some(true)
      anUnauthorisedPaymentsEmptyViewModel
        .copy(surchargeQuestion = None, noSurchargeQuestion = Some(true))
        .unauthorisedPaymentQuestion shouldBe Some(true)
      anUnauthorisedPaymentsEmptyViewModel
        .copy(surchargeQuestion = Some(true), noSurchargeQuestion = Some(true))
        .unauthorisedPaymentQuestion shouldBe Some(true)
    }
    "return 'false' when surchargeQuestion and/or noSurchargeQuestion are 'false'" in {
      anUnauthorisedPaymentsViewModel
        .copy(surchargeQuestion = Some(false), noSurchargeQuestion = Some(false))
        .unauthorisedPaymentQuestion
        .get shouldBe false
      anUnauthorisedPaymentsViewModel
        .copy(surchargeQuestion = None, noSurchargeQuestion = Some(false))
        .unauthorisedPaymentQuestion
        .get shouldBe false
      anUnauthorisedPaymentsViewModel
        .copy(surchargeQuestion = Some(false), noSurchargeQuestion = None)
        .unauthorisedPaymentQuestion
        .get shouldBe false
    }
    "return 'None' when surchargeQuestion and noSurchargeQuestion are 'None'" in {
      anUnauthorisedPaymentsEmptyViewModel.unauthorisedPaymentQuestion shouldBe None
    }
  }
}
