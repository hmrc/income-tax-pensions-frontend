package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, spAndSpLumpSum, statePensionLumpSumOnly}
import builders.StateBenefitViewModelBuilder.{aStatePensionLumpSumViewModel, aStatePensionViewModel}
import cats.implicits.{catsSyntaxOptionId, none}
import utils.UnitTest

import java.time.LocalDate

class incomeFromPensionsSpec extends UnitTest {

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
