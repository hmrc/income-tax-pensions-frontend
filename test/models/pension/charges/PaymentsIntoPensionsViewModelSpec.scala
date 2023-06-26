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

import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import support.UnitTest

class PaymentsIntoPensionsViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPaymentsIntoPensionViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      !aPaymentsIntoPensionViewModel.isEmpty
      !aPaymentsIntoPensionViewModel.copy(totalWorkplacePensionPayments = None).isEmpty
      !aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true)).isEmpty
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPaymentsIntoPensionViewModel.isFinished
      }
      "all required questions are answered" in {
        aPaymentsIntoPensionViewModel.copy(
          rasPensionPaymentQuestion = Some(false),
          totalRASPaymentsAndTaxRelief = None,
          oneOffRasPaymentPlusTaxReliefQuestion = None,
          totalOneOffRasPaymentPlusTaxRelief = None,
          totalPaymentsIntoRASQuestion = None,
        ).isFinished
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPaymentsIntoPensionViewModel.copy(totalRASPaymentsAndTaxRelief = None).isFinished
      }
    }
  }
}
