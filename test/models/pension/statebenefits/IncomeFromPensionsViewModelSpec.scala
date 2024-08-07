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

package models.pension.statebenefits

import builders.IncomeFromPensionsViewModelBuilder._
import builders.StateBenefitViewModelBuilder._
import cats.implicits.{catsSyntaxOptionId, none}
import utils.UnitTest

import java.time.LocalDate

class IncomeFromPensionsViewModelSpec extends UnitTest {

  ".isStatePensionFinished" should {
    "return true" when {
      "both state pension and lump sum are finished" in {
        spAndSpLumpSum.isStatePensionFinished shouldBe true
      }
      "only one of the journeys has been finished" in {
        statePensionLumpSumOnly.isStatePensionFinished shouldBe true
      }
    }
    "return false" when {
      "neither state pension nor lump sum have been started" in {
        anIncomeFromPensionEmptyViewModel.isStatePensionFinished shouldBe false
      }
      "both journeys started but not finished" in {
        anIncomeFromPensionEmptyViewModel
          .copy(
            statePension = aStatePensionViewModel.copy(startDate = none[LocalDate]).some,
            statePensionLumpSum = aStatePensionLumpSumViewModel.copy(startDate = none[LocalDate]).some
          )
          .isStatePensionFinished shouldBe false
      }
    }
  }

  ".isEmpty" should {
    "return true when no questions have been answered" in {
      anIncomeFromPensionEmptyViewModel.isEmpty
    }
    "return false when any questions have been answered" in {
      anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true)).isEmpty shouldBe false
      anIncomeFromPensionsViewModel.isEmpty shouldBe false
    }
  }

  ".isFinished" should {
    "return true" when {
      "all required parameters are populated" in {
        aStatePensionIncomeFromPensionsViewModel.isStatePensionFinished shouldBe true
        IncomeFromPensionsViewModel(
          statePension = Some(aMinimalStatePensionViewModel),
          statePensionLumpSum = Some(aMinimalStatePensionLumpSumViewModel)
        ).isStatePensionFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aStatePensionIncomeFromPensionsViewModel.copy(statePension = None, statePensionLumpSum = None).isStatePensionFinished shouldBe false

        aStatePensionIncomeFromPensionsViewModel
          .copy(
            statePension = Some(aMinimalStatePensionViewModel.copy(amountPaidQuestion = Some(true))),
            statePensionLumpSum = Some(aMinimalStatePensionViewModel.copy(amountPaidQuestion = Some(true)))
          )
          .isStatePensionFinished shouldBe false
      }
    }
  }

  ".journeyIsNoStatePension" should {
    "return true" when {
      "StatePension and StatePensionLumpSum amountPaidQuestion are Some(false)" in {
        IncomeFromPensionsViewModel(
          statePension = Some(aMinimalStatePensionViewModel),
          statePensionLumpSum = Some(aMinimalStatePensionLumpSumViewModel)
        ).journeyIsNoStatePension
      }
    }
    "return false" when {
      "StatePension and StatePensionLumpSum amountPaidQuestion are not Some(false)" in {
        aStatePensionIncomeFromPensionsViewModel
          .copy(
            statePension = None,
            statePensionLumpSum = Some(aMinimalStatePensionLumpSumViewModel)
          )
          .journeyIsNoStatePension shouldBe false
        aStatePensionIncomeFromPensionsViewModel.journeyIsNoStatePension shouldBe false
      }
    }
  }

}
