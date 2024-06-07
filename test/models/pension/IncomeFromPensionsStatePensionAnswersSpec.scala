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

package models.pension

import builders.StateBenefitViewModelBuilder.{aStatePensionLumpSumViewModel, anStateBenefitViewModel}
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.scalatest.wordspec.AnyWordSpecLike

class IncomeFromPensionsStatePensionAnswersSpec extends AnyWordSpecLike {
  "toIncomeFromPensionsViewModel" should {
    "convert all Nones to an empty view model" in {
      val actual = IncomeFromPensionsStatePensionAnswers.empty.toIncomeFromPensionsViewModel
      assert(actual === IncomeFromPensionsViewModel(None, None, None, None))
    }

    "convert pension answers to the view model" in {
      val answers = IncomeFromPensionsStatePensionAnswers(
        Some(anStateBenefitViewModel),
        Some(aStatePensionLumpSumViewModel),
        Some("sessionId")
      )
      val actual = answers.toIncomeFromPensionsViewModel
      assert(actual === IncomeFromPensionsViewModel(Some(anStateBenefitViewModel), Some(aStatePensionLumpSumViewModel), None, None))
    }
  }
}
