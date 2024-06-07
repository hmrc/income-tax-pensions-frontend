package models.pension

import builders.StateBenefitViewModelBuilder
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
