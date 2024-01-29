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

package services.redirects

import builders.IncomeFromPensionsViewModelBuilder.aStatePensionIncomeFromPensionsViewModel
import builders.StateBenefitViewModelBuilder.{aMinimalStatePensionViewModel, aStatePensionViewModel, anEmptyStateBenefitViewModel}
import controllers.pensions.incomeFromPensions.routes.{StatePensionCYAController, StatePensionController}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.mvc.Results.Redirect
import services.redirects.StatePensionPages._
import services.redirects.StatePensionRedirects.{cyaPageCall, journeyCheck}
import utils.UnitTest

class StatePensionRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val someRedirect              = Some(Redirect(StatePensionController.show(taxYear)))

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val data   = cyaData.copy(incomeFromPensions = IncomeFromPensionsViewModel(statePension = Some(anEmptyStateBenefitViewModel)))
        val result = journeyCheck(DoYouGetRegularStatePaymentsPage, data, taxYear)

        result shouldBe None
      }

      "current page is pre-filled and at end of journey so far" in {
        val data = cyaData.copy(
          incomeFromPensions = IncomeFromPensionsViewModel(statePension =
            Some(anEmptyStateBenefitViewModel.copy(amountPaidQuestion = Some(true), amount = Some(155.88))))
        )
        val result = journeyCheck(DoYouGetRegularStatePaymentsPage, data, taxYear)

        result shouldBe None
      }

      "current page is pre-filled and mid-journey" in {
        val data   = cyaData.copy(incomeFromPensions = aStatePensionIncomeFromPensionsViewModel)
        val result = journeyCheck(TaxOnStatePensionLumpSumPage, data, taxYear)

        result shouldBe None
      }

      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data   = cyaData.copy(incomeFromPensions = IncomeFromPensionsViewModel(statePension = Some(aMinimalStatePensionViewModel)))
        val result = journeyCheck(StatePensionLumpSumPage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data = cyaData.copy(
          incomeFromPensions = aStatePensionIncomeFromPensionsViewModel.copy(
            statePension = Some(
              aStatePensionViewModel.copy(
                amountPaidQuestion = None,
                amount = None
              )))
        )
        val result = journeyCheck(StatePensionLumpSumPage, data, taxYear)

        result shouldBe someRedirect
      }
      "current page is invalid in journey" in {
        val data   = cyaData.copy(incomeFromPensions = IncomeFromPensionsViewModel(statePension = Some(aMinimalStatePensionViewModel)))
        val result = journeyCheck(WhenDidYouStartGettingStatePaymentsPage, data, taxYear)

        result shouldBe someRedirect
      }
    }
  }

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe StatePensionCYAController.show(taxYear)
    }
  }

}
