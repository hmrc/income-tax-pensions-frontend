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

package models.pension.reliefs

import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import support.UnitTest

class PaymentsIntoPensionsViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when no questions have been answered" in {
      aPaymentsIntoPensionsEmptyViewModel.isEmpty
    }
    "return false when any questions have been answered" in {
      aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true)).isEmpty shouldBe false
      aPaymentsIntoPensionViewModel.isEmpty shouldBe false
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPaymentsIntoPensionViewModel.isFinished
      }
      "all required questions are answered" in {
        aPaymentsIntoPensionViewModel
          .copy(
            retirementAnnuityContractPaymentsQuestion = Some(false),
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = Some(false),
            totalWorkplacePensionPayments = None
          )
          .isFinished
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        !aPaymentsIntoPensionViewModel.copy(totalWorkplacePensionPayments = None).isFinished
      }
    }
  }

  "journeyIsNo" should {
    "return true when rasPensionPaymentQuestion is 'false' and no others have been answered" in {
      aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      aPaymentsIntoPensionsEmptyViewModel.journeyIsNo shouldBe false
      aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true)).journeyIsNo shouldBe false
      aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(false)).journeyIsNo shouldBe false
    }
  }

  "toReliefs" should {
    "set BigDecimal values to 0.0 if None" in {
      assert(aPaymentsIntoPensionsEmptyViewModel.toReliefs === Reliefs(Some(0.0), Some(0.0), Some(0.0), Some(0.0), None))
    }

    "set BigDecimal values if defined" in {
      assert(
        PaymentsIntoPensionsViewModel(
          Some(true),
          Some(1.0),
          Some(true),
          Some(2.0),
          Some(true),
          Some(true),
          Some(true),
          Some(3.0),
          Some(true),
          Some(4.0)
        ).toReliefs === Reliefs(Some(1.0), Some(2.0), Some(3.0), Some(4.0), None))
    }

  }
}
