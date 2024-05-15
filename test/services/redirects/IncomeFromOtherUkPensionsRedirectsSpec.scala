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

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import controllers.pensions.incomeFromPensions.routes._
import models.mongo.PensionsUserData
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.IncomeFromOtherUkPensionsPages._
import services.redirects.IncomeFromOtherUkPensionsRedirects._
import utils.UnitTest

import scala.concurrent.Future

class IncomeFromOtherUkPensionsRedirectsSpec extends UnitTest {

  private val journeyStartCall: Call                      = UkPensionSchemePaymentsController.show(taxYear)
  private val schemeStartCall: Call                       = PensionSchemeDetailsController.show(taxYear, None)
  private val howMuchPaidCall: Call                       = PensionAmountController.show(taxYear, Some(0))
  private val whenDidYouStartGettingPaymentPageCall: Call = PensionSchemeStartDateController.show(taxYear, Some(0))
  private val schemeDetailsCall: Call                     = PensionSchemeDetailsController.show(taxYear, Some(0))
  private val schemeSummaryCall: Call                     = UkPensionIncomeSummaryController.show(taxYear)
  private val removeSchemeCall: Call                      = RemovePensionSchemeController.show(taxYear, Some(0))
  private val checkYourAnswersCall: Call                  = UkPensionIncomeCYAController.show(currentTaxYear)
  private val journeyStartRedirect: Some[Result]          = Some(Redirect(journeyStartCall))
  private def continueToContextualRedirect(continue: Call): PensionsUserData => Future[Result] = _ => Future.successful(Redirect(continue))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }

  ".indexCheckThenJourneyCheck" when {
    "index is valid" should {
      "return PensionsUserData if previous questions are answered and journey is valid" when {
        "accessing the first page in the scheme loop" in {
          val result =
            indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(0), currentPage = PensionSchemeDetailsPage, taxYear = taxYear)(
              continueToContextualRedirect(schemeStartCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(schemeStartCall.url)
        }
        "accessing a page in the scheme loop" in {
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData,
            optIndex = Some(0),
            currentPage = WhenDidYouStartGettingPaymentsPage,
            taxYear = taxYear)(continueToContextualRedirect(whenDidYouStartGettingPaymentPageCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(whenDidYouStartGettingPaymentPageCall.url)
        }
        "accessing the scheme summary page" in {
          val result = indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(0), currentPage = UkPensionIncomePage, taxYear = taxYear)(
            continueToContextualRedirect(schemeSummaryCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(schemeSummaryCall.url)
        }
        "accessing the remove scheme page" in {
          val result =
            indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(0), currentPage = RemovePensionIncomePage, taxYear = taxYear)(
              continueToContextualRedirect(removeSchemeCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(removeSchemeCall.url)
        }
        "accessing the CYA page" in {
          val result =
            indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = None, currentPage = CheckUkPensionIncomeCYAPage, taxYear = taxYear)(
              continueToContextualRedirect(checkYourAnswersCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(checkYourAnswersCall.url)
        }
      }

      "redirect to first page in journey" when {
        "previous questions are unanswered" in {
          val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(amount = None)))
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(incompleteJourney),
            optIndex = Some(0),
            currentPage = WhenDidYouStartGettingPaymentsPage,
            taxYear = taxYear)(continueToContextualRedirect(whenDidYouStartGettingPaymentPageCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "journey is invalid" in {
          val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = None)
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(invalidJourney),
            optIndex = Some(1),
            currentPage = WhenDidYouStartGettingPaymentsPage,
            taxYear = taxYear)(continueToContextualRedirect(whenDidYouStartGettingPaymentPageCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
      }
    }

    "index is invalid" should {
      "redirect to the first page in journey" when {
        "previous questions are unanswered" in {
          val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(pensionId = None)))
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(incompleteJourney),
            optIndex = Some(10),
            currentPage = SchemeSummaryPage,
            taxYear = taxYear)(continueToContextualRedirect(schemeDetailsCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "page is invalid" in {
          val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = None)
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(invalidJourney),
            optIndex = Some(-1),
            currentPage = HowMuchPensionDidYouGetPaidPage,
            taxYear = taxYear)(continueToContextualRedirect(howMuchPaidCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "there are existing schemes" in {
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(aUKIncomeFromPensionsViewModel),
            optIndex = Some(-1),
            currentPage = HowMuchPensionDidYouGetPaidPage,
            taxYear = taxYear
          )(continueToContextualRedirect(howMuchPaidCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "there are no schemes" in {
          val emptySchemesViewModel = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq.empty)
          val result = indexCheckThenJourneyCheck(
            data = pensionsUserDataWithIncomeFromPensions(emptySchemesViewModel),
            optIndex = Some(-1),
            currentPage = HowMuchPensionDidYouGetPaidPage,
            taxYear = taxYear)(continueToContextualRedirect(howMuchPaidCall))
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
      }

      "redirect to the scheme summary page when trying to access the RemovePSTR page" in {
        val result: Future[Result] =
          indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(8), currentPage = RemovePensionIncomePage, taxYear = taxYear)(
            continueToContextualRedirect(removeSchemeCall))
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeSummaryCall.url)
      }
    }
  }

  ".journeyCheck" should {
    "return None if previous questions are answered and journey is valid" in {
      val result = journeyCheck(currentPage = WhenDidYouStartGettingPaymentsPage, cya = aPensionsCYAModel, taxYear = taxYear, optIndex = Some(0))

      result shouldBe None
    }
    "return Some(Redirect) to first page in journey" when {
      "previous questions are unanswered" in {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(amount = None)))
        val result = journeyCheck(
          currentPage = WhenDidYouStartGettingPaymentsPage,
          cya = pensionsUserDataWithIncomeFromPensions(incompleteJourney).pensions,
          taxYear = taxYear,
          optIndex = Some(0)
        )

        result shouldBe journeyStartRedirect
      }
      "journey is invalid" in {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = None)
        val result = journeyCheck(
          currentPage = WhenDidYouStartGettingPaymentsPage,
          cya = pensionsUserDataWithIncomeFromPensions(invalidJourney).pensions,
          taxYear = taxYear,
          optIndex = Some(0))

        result shouldBe journeyStartRedirect
      }
    }
  }

  ".redirectForSchemeLoop" should {
    "filter incomplete schemes and return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[UkPensionIncomeViewModel]      = Seq.empty
      val incompleteSchemes: Seq[UkPensionIncomeViewModel] = Seq(anUkPensionIncomeViewModelOne.copy(amount = None, taxPaid = None))
      val result1                                          = redirectForSchemeLoop(emptySchemes, taxYear)
      val result2                                          = redirectForSchemeLoop(incompleteSchemes, taxYear)

      result1 shouldBe schemeStartCall
      result2 shouldBe schemeStartCall
    }
    "filter incomplete schemes and return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[UkPensionIncomeViewModel] =
        Seq(anUkPensionIncomeViewModelOne.copy(amount = None, taxPaid = None), anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo)
      val result = redirectForSchemeLoop(existingSchemes, taxYear)

      result shouldBe schemeSummaryCall
    }
  }

}
