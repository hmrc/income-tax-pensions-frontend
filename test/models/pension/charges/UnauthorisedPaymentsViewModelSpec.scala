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

import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
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
                ukPensionSchemesQuestion = None, pensionSchemeTaxReference = None )
        anUnauthorisedPaymentsViewModel.copyWithQuestionsApplied(Some(false), Some(false)) shouldBe expected
      }
    }
  }
}
