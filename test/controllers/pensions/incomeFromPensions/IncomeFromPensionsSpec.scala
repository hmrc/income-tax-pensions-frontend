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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder._
import builders.StateBenefitViewModelBuilder.{aStatePensionLumpSumViewModel, aStatePensionViewModel}
import cats.implicits.{catsSyntaxOptionId, none}
import utils.UnitTest

import java.time.LocalDate

class IncomeFromPensionsSpec extends UnitTest {

  "checking whether state pension claims are complete" should {
    "return true" when {
      "both state pension and lump sum are finished" in {
        areStatePensionClaimsComplete(spAndSpLumpSum) shouldBe true
      }
    }
    "return false" when {
      "neither state pension nor lump sum have been started" in {
        areStatePensionClaimsComplete(anIncomeFromPensionEmptyViewModel) shouldBe false
      }
      "both journeys started but not finished" in {
        val answers = anIncomeFromPensionEmptyViewModel
          .copy(
            statePension = aStatePensionViewModel.copy(startDate = none[LocalDate]).some,
            statePensionLumpSum = aStatePensionLumpSumViewModel.copy(startDate = none[LocalDate]).some
          )
        areStatePensionClaimsComplete(answers) shouldBe false
      }
      "only one of the journeys has been finished" in {
        areStatePensionClaimsComplete(statePensionLumpSumOnly) shouldBe false
      }
    }
  }

}
