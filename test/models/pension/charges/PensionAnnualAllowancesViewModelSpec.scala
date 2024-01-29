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

import builders.PensionAnnualAllowanceViewModelBuilder.{aPensionAnnualAllowanceEmptyViewModel, aPensionAnnualAllowanceViewModel}
import utils.UnitTest

class PensionAnnualAllowancesViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPensionAnnualAllowanceEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aPensionAnnualAllowanceViewModel.isEmpty shouldBe false
      aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = None).isEmpty shouldBe false
      aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(false)).isEmpty shouldBe false
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPensionAnnualAllowanceViewModel.isFinished
      }
      "all required questions are answered" in {
        aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(false)).isFinished shouldBe true
        aPensionAnnualAllowanceEmptyViewModel
          .copy(
            reducedAnnualAllowanceQuestion = Some(true),
            moneyPurchaseAnnualAllowance = Some(true),
            taperedAnnualAllowance = None,
            aboveAnnualAllowanceQuestion = Some(false)
          )
          .isFinished shouldBe true
        aPensionAnnualAllowanceViewModel
          .copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false), taxPaidByPensionProvider = None)
          .isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPensionAnnualAllowanceEmptyViewModel.isFinished shouldBe false
        aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(true)).isFinished shouldBe false
        aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq.empty)).isFinished shouldBe false
        aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = None).isFinished shouldBe false
        aPensionAnnualAllowanceViewModel
          .copy(moneyPurchaseAnnualAllowance = Some(false), taperedAnnualAllowance = Some(false))
          .isFinished shouldBe false
      }
    }
  }

  "journeyIsNo" should {
    "return true when shortServiceRefund is 'false' and no others have been answered" in {
      aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      aPensionAnnualAllowanceEmptyViewModel.journeyIsNo shouldBe false
      aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(true)).journeyIsNo shouldBe false
      aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false)).journeyIsNo shouldBe false
    }
  }

  "journeyIsUnanswered" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPensionAnnualAllowanceEmptyViewModel.journeyIsUnanswered
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aPensionAnnualAllowanceViewModel.journeyIsUnanswered shouldBe false
      aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = None).journeyIsUnanswered shouldBe false
      aPensionAnnualAllowanceEmptyViewModel.copy(reducedAnnualAllowanceQuestion = Some(false)).journeyIsUnanswered shouldBe false
    }
  }
}
