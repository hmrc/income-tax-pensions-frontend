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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.StateBenefitViewModelBuilder.{anStateBenefitViewModelOne, anStateBenefitViewModelTwo}
import models.mongo.PensionsCYAModel
import play.api.mvc.Results.Redirect
import utils.UnitTest
import controllers.pensions.incomeFromPensions.routes.{StatePensionCYAController, StatePensionController}
import services.redirects.StatePensionPages.{
  AddStatePensionToIncomeTaxCalcPage, StatePensionPage, StatePensionLumpSumPage, WhenDidYouStartGettingStatePaymentsPage}
import services.redirects.StatePensionRedirects.{cyaPageCall, journeyCheck}

class StatePensionRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val someRedirect = Some(Redirect(StatePensionController.show(taxYear)))

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val data = cyaData.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(
              amountPaidQuestion = None))
          ))
        val result = journeyCheck(StatePensionPage, data, taxYear)

        result shouldBe None
      }

      "current page is pre-filled and at end of journey so far" in {
        val data = cyaData.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(
              amountPaidQuestion = Some(true),
              amount = Some(155.88),
              startDate = None
            ))
          )
        )
        val result = journeyCheck(StatePensionPage, data, taxYear)

        result shouldBe None
      }

      "current page is pre-filled and mid-journey" in {
        val data = cyaData.copy(incomeFromPensions = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne)
        ))
        val result = journeyCheck(AddStatePensionToIncomeTaxCalcPage, data, taxYear)

        result shouldBe None
      }

      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data = cyaData.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(
              amountPaidQuestion = Some(false),
              amount = None,
              startDateQuestion = Some(true)
            ))
          )
        )
        val result = journeyCheck(StatePensionPage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data = cyaData.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelTwo.copy(
              amountPaidQuestion = None,
              amount = None,
              startDateQuestion = Some(true)
            ))
          )
        )
        val result = journeyCheck(StatePensionLumpSumPage, data, taxYear)

        result shouldBe someRedirect
      }
      "current page is invalid in journey" in {
        val data = cyaData.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelTwo.copy(
            amountPaidQuestion = Some(false),
            amount = None
          ))
          )
        )
        val result = journeyCheck(WhenDidYouStartGettingStatePaymentsPage, data, taxYear)

        result shouldBe someRedirect
      }

    }


      ".cyaPageCall" should {
        "return a redirect call to the cya page" in {
          cyaPageCall(taxYear) shouldBe StatePensionCYAController.show(taxYear)
        }
      }

    }

}
