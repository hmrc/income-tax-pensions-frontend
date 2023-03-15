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
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsNonUkEmptySchemeViewModel
import controllers.ControllerSpec.UserConfig
import controllers.YesNoControllerSpec
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}
import play.api.http.Status.BAD_REQUEST
import play.api.libs.ws.WSResponse

class TaxOnShortServiceRefundControllerISpec extends YesNoControllerSpec("/overseas-pensions/short-service-refunds/short-service-refunds-uk-tax"){
  "This page" when {

    ".show" should {
      "not show the form page but redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.overseasPensionsSummary)
        }
      }
    }

    ".submit" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

          assertRedirectionAsExpected(PageRelativeURLs.overseasPensionsSummary)
          getTransferPensionsViewModel mustBe None
        }
      }

      "succeed" when {
        "the user has relevant session data and" when {

          "the user has selected 'Yes'" in {
            val sessionData = pensionsUserData(aPensionsCYAModel)
            val expectedViewModel : ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)), queryParams = Map("refundPensionSchemeIndex" -> "0"))

            val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

            assertRedirectionAsExpected(redirectPage)
            getShortServiceViewModel mustBe Some(expectedViewModel)
          }

          "the user has selected 'No'" in {
            val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsNonUkEmptySchemeViewModel))
            val expectedViewModel = sessionData.pensions.shortServiceRefunds

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)), queryParams = Map("refundPensionSchemeIndex" -> "0"))

            val redirectPage = relativeUrl(
              "/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0")

            assertRedirectionAsExpected(redirectPage)
            getShortServiceViewModel mustBe Some(expectedViewModel)
          }

          "the user has not selected any option" in {
            val sessionData = pensionsUserData(aPensionsCYAModel)
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
          }

          "the user selects 'No' when previously 'Yes' was submitted and it clears PensionScheme data" in {
            val sessionData = pensionsUserData(aPensionsCYAModel)

            val expectedViewModel : ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds.copy(
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

            val expectedViewModel : ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds.copy(
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
      }
    }
  }
}