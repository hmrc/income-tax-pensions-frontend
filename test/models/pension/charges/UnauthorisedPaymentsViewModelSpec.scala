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

import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import support.UnitTest

class UnauthorisedPaymentsViewModelSpec extends UnitTest {

  "copyWithQuestionsApplied" should {
    "copy the content of the model without modifying" when {
      "the questions are set to true" in {
        anUnauthorisedPaymentsViewModel.copyWithQuestionsApplied(Some(true), Some(true)) shouldBe anUnauthorisedPaymentsViewModel
      }
    }

    "copy the model and set the values to None" when {
      "the 'surcharge' question is false" in {
        val expected = anUnauthorisedPaymentsViewModel
          .copy(surchargeQuestion = Some(false), surchargeAmount = None, surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
        anUnauthorisedPaymentsViewModel.copyWithQuestionsApplied(Some(false), Some(true)) shouldBe expected
      }

      "the 'no surcharge' question is false" in {
        val expected = anUnauthorisedPaymentsViewModel
          .copy(noSurchargeQuestion = Some(false), noSurchargeAmount = None, noSurchargeTaxAmountQuestion = None, noSurchargeTaxAmount = None)
        anUnauthorisedPaymentsViewModel.copyWithQuestionsApplied(Some(true), Some(false)) shouldBe expected
      }

      "both 'surcharge' and 'no surcharge' are false" in {
        val expected = anUnauthorisedPaymentsViewModel
          .copy(surchargeQuestion = Some(false), surchargeAmount = None, surchargeTaxAmountQuestion = None, surchargeTaxAmount = None,
            noSurchargeQuestion = Some(false), noSurchargeAmount = None, noSurchargeTaxAmountQuestion = None, noSurchargeTaxAmount = None,
            ukPensionSchemesQuestion = None, pensionSchemeTaxReference = None)
        anUnauthorisedPaymentsViewModel.copyWithQuestionsApplied(Some(false), Some(false)) shouldBe expected
      }
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        anUnauthorisedPaymentsViewModel.isFinished
      }
      "all required questions are answered" in {
        anUnauthorisedPaymentsViewModel.copy(
          surchargeQuestion = Some(false),
          surchargeAmount = None,
          surchargeTaxAmountQuestion = None,
          surchargeTaxAmount = None,
        ).isFinished shouldBe true

        anUnauthorisedPaymentsViewModel.copy(
          surchargeTaxAmountQuestion = Some(false),
          surchargeTaxAmount = None,
          noSurchargeQuestion = Some(false),
          noSurchargeAmount = None,
          noSurchargeTaxAmountQuestion = None,
          noSurchargeTaxAmount = None,
          ukPensionSchemesQuestion = Some(false),
          pensionSchemeTaxReference = None
        ).isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        anUnauthorisedPaymentsViewModel.copy(
          surchargeQuestion = Some(true),
          surchargeAmount = None,
          surchargeTaxAmountQuestion = Some(false),
          surchargeTaxAmount = None,
        ).isFinished
      }
    }
  }

  "toUnauth should transform an UnauthorisedPaymentsViewModel into a PensionSchemeUnauthorisedPayments model that" should {
    "contain the PSTR, surcharge and non-surcharge data" in {
      val expectedResult: PensionSchemeUnauthorisedPayments = PensionSchemeUnauthorisedPayments(
        Some(Seq("12345678AB", "12345678AC")),
        Some(Charge(12.11, 34.22)),
        Some(Charge(88.11, 99.22))
      )

      anUnauthorisedPaymentsViewModel.toUnauth shouldBe expectedResult
    }

    "contain the partial data that is present in the view model" in {
      val viewModel: UnauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(
        noSurchargeQuestion = Some(false), noSurchargeAmount = None, noSurchargeTaxAmountQuestion = None, noSurchargeTaxAmount = None)
      val expectedResult: PensionSchemeUnauthorisedPayments = PensionSchemeUnauthorisedPayments(
        Some(Seq("12345678AB", "12345678AC")),
        Some(Charge(12.11, 34.22)),
        None
      )

      viewModel.toUnauth shouldBe expectedResult
    }

    "be empty when there is no surcharge or non-surcharge payments" in {
      val viewModel: UnauthorisedPaymentsViewModel = anUnauthorisedPaymentsEmptyViewModel.copy(
        surchargeQuestion = Some(false), noSurchargeQuestion = Some(false))
      val expectedResult: PensionSchemeUnauthorisedPayments = PensionSchemeUnauthorisedPayments(None, None, None)

      viewModel.toUnauth shouldBe expectedResult
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
      anUnauthorisedPaymentsEmptyViewModel.copy(surchargeQuestion = Some(true), noSurchargeQuestion = Some(false)).unauthorisedPaymentQuestion shouldBe Some(true)
      anUnauthorisedPaymentsEmptyViewModel.copy(surchargeQuestion = None, noSurchargeQuestion = Some(true)).unauthorisedPaymentQuestion shouldBe Some(true)
      anUnauthorisedPaymentsEmptyViewModel.copy(surchargeQuestion = Some(true), noSurchargeQuestion = Some(true)).unauthorisedPaymentQuestion shouldBe Some(true)
    }
    "return 'false' when surchargeQuestion and/or noSurchargeQuestion are 'false'" in {
      anUnauthorisedPaymentsViewModel.copy(surchargeQuestion = Some(false), noSurchargeQuestion = Some(false)).unauthorisedPaymentQuestion.get shouldBe false
      anUnauthorisedPaymentsViewModel.copy(surchargeQuestion = None, noSurchargeQuestion = Some(false)).unauthorisedPaymentQuestion.get shouldBe false
      anUnauthorisedPaymentsViewModel.copy(surchargeQuestion = Some(false), noSurchargeQuestion = None).unauthorisedPaymentQuestion.get shouldBe false
    }
    "return 'None' when surchargeQuestion and noSurchargeQuestion are 'None'" in {
      anUnauthorisedPaymentsEmptyViewModel.unauthorisedPaymentQuestion shouldBe None
    }
  }
}
