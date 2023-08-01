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

import builders.LifetimeAllowanceBuilder.aLifetimeAllowance1
import builders.PensionLifetimeAllowancesViewModelBuilder.{aPensionLifetimeAllowancesEmptyViewModel, aPensionLifetimeAllowancesViewModel}
import utils.UnitTest

class PensionLifetimeAllowancesViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPensionLifetimeAllowancesEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aPensionLifetimeAllowancesViewModel.isEmpty shouldBe false
      aPensionLifetimeAllowancesViewModel.copy(pensionSchemeTaxReferences = None).isEmpty shouldBe false
      aPensionLifetimeAllowancesEmptyViewModel.copy(aboveLifetimeAllowanceQuestion = Some(false)).isEmpty shouldBe false
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPensionLifetimeAllowancesViewModel.isFinished
      }
      "all required questions are answered" in {
        aPensionLifetimeAllowancesEmptyViewModel.copy(aboveLifetimeAllowanceQuestion = Some(false)).isFinished shouldBe true
        aPensionLifetimeAllowancesEmptyViewModel.copy(
          aboveLifetimeAllowanceQuestion = Some(true),
          pensionAsLumpSumQuestion = Some(true),
          pensionAsLumpSum = Some(aLifetimeAllowance1),
          pensionPaidAnotherWayQuestion = Some(false)).isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPensionLifetimeAllowancesEmptyViewModel.isFinished shouldBe false
        aPensionLifetimeAllowancesEmptyViewModel.copy(aboveLifetimeAllowanceQuestion = Some(true)).isFinished shouldBe false
        aPensionLifetimeAllowancesViewModel.copy(pensionSchemeTaxReferences = Some(Seq.empty)).isFinished shouldBe false
        aPensionLifetimeAllowancesViewModel.copy(pensionAsLumpSumQuestion = None).isFinished shouldBe false
        aPensionLifetimeAllowancesViewModel.copy(
          pensionPaidAnotherWay = Some(aLifetimeAllowance1.copy(amount = None))).isFinished shouldBe false
      }
    }
  }

}
