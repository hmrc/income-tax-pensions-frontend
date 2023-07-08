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

package controllers.pensions.shortServiceRefunds

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsEmptySchemeViewModel, aShortServiceRefundsNonUkEmptySchemeViewModel, aShortServiceRefundsViewModel, minimalShortServiceRefundsViewModel}
import controllers.ControllerSpec.UserConfig
import controllers.YesNoControllerSpec
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class TaxOnShortServiceRefundControllerISpec extends YesNoControllerSpec("/overseas-pensions/short-service-refunds/short-service-refunds-uk-tax") {

  ".show" should {

    "show page when EOY" when {
      "has a valid index" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(1)

        response.status equals OK
      }
      "has no index" in {
        val emptySchemeCYAModel = aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq.empty))
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex()

        response.status equals OK
      }
    }

    "redirect to the first page in journey" when {
      "previous questions have not been answered" in {
        val incompleteCYAModel = aPensionsCYAModel.copy(shortServiceRefunds = ShortServiceRefundsViewModel(shortServiceRefund = Some(true)))
        val sessionData = pensionsUserData(incompleteCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex()

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
      "page is invalid in journey" in {
        val invalidCYAModel = aPensionsCYAModel.copy(shortServiceRefunds = ShortServiceRefundsViewModel(shortServiceRefund = Some(false)))
        val sessionData = pensionsUserData(invalidCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex()

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
    }

    "redirect to the summary page" when {
      "the user has no stored session data at all" in {
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
        implicit val response: WSResponse = getPageWithIndex()
        assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      }
    }
  }

  ".submit" should {
    "succeed when the user has relevant session data and" when {

        "the user has selected 'Yes' with no index" in {
          val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
          val expectedViewModel: ShortServiceRefundsViewModel = aShortServiceRefundsEmptySchemeViewModel.copy(
            refundPensionScheme = Seq(OverseasRefundPensionScheme(ukRefundCharge = Some(true)))
          )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> ""))

          val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

          assertRedirectionAsExpected(redirectPage)
          getShortServiceViewModel mustBe Some(expectedViewModel)
        }

        "the user has selected 'No' with no index" in {
          val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
          val expectedViewModel: ShortServiceRefundsViewModel = aShortServiceRefundsEmptySchemeViewModel.copy(
            refundPensionScheme = Seq(OverseasRefundPensionScheme(ukRefundCharge = Some(false)))
          )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)), queryParams = Map("refundPensionSchemeIndex" -> ""))

          val redirectPage = relativeUrl(
            "/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

          assertRedirectionAsExpected(redirectPage)
          getShortServiceViewModel mustBe Some(expectedViewModel)
        }

        "the user selects 'No' when previously 'Yes' was submitted and it clears PensionScheme data" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)

          val expectedViewModel: ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds.copy(
            refundPensionScheme = sessionData.pensions.shortServiceRefunds.refundPensionScheme.updated(0,
              OverseasRefundPensionScheme(
                ukRefundCharge = Some(false)
              )
            )
          )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)), queryParams = Map("refundPensionSchemeIndex" -> "0"))

          val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

          assertRedirectionAsExpected(redirectPage)
          getShortServiceViewModel mustBe Some(expectedViewModel)
        }

        "the user selects 'Yes' when previously 'No' was submitted and it clears PensionScheme data" in {
          val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsNonUkEmptySchemeViewModel))

          val expectedViewModel: ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds.copy(
            refundPensionScheme = sessionData.pensions.shortServiceRefunds.refundPensionScheme.updated(0,
              OverseasRefundPensionScheme(
                ukRefundCharge = Some(true)
              )
            )
          )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> "0"))

          val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

          assertRedirectionAsExpected(redirectPage)
          getShortServiceViewModel mustBe Some(expectedViewModel)
        }
      }

    "return BAD_REQUEST when the user has not selected any option" in {
      val sessionData = pensionsUserData(aPensionsCYAModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

      response must haveStatus(BAD_REQUEST)
    }

    "redirect to first page of journey" when {
      "page is invalid" in {
        val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = minimalShortServiceRefundsViewModel))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> ""))

        val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/taxable-short-service-refunds")

        assertRedirectionAsExpected(redirectPage)
      }
      "previous questions are unanswered" in {
        val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel.copy(
          shortServiceRefundTaxPaidCharge = None
        )))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> ""))

        val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/taxable-short-service-refunds")

        assertRedirectionAsExpected(redirectPage)
      }
    }

    "redirect to first page of scheme loop (itself) without an index when submission index is invalid and there are no schemes" in {
      val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> "10"))

      val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-uk-tax")

      assertRedirectionAsExpected(redirectPage)
    }

    "redirect to scheme summary page when submission index is invalid and there are existing schemes" in {
      val sessionData = pensionsUserData(aPensionsCYAModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> "-1"))

      val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refund-summary")

      assertRedirectionAsExpected(redirectPage)
    }

    "redirect to the Pensions Summary page when the user has no stored session data at all" in {

      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

      assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      getTransferPensionsViewModel mustBe None
    }
  }
}
