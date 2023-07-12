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

import builders.OverseasRefundPensionSchemeBuilder.anOverseasRefundPensionSchemeWithoutUkRefundCharge
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsEmptySchemeViewModel, aShortServiceRefundsViewModel}
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse

class RemoveRefundSchemeControllerISpec extends ControllerSpec("/overseas-pensions/short-service-refunds/remove-pension-scheme") {

  "show" should {
    "show page when using a valid index" in {
      val sessionData = pensionsUserData(aPensionsCYAModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = getPageWithIndex(1)

      response.status equals OK
    }

    "redirect to the summary page" when {
      "the user has no stored session data at all" in {
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
        implicit val response: WSResponse = getPageWithIndex()
        assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      }
    }

    "redirect to refund scheme summary page" when {
      "the user accesses page with index out of bounds" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(10)

        assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
      }
    }
  }

  "submit" should {

    "redirect to the scheme summary page" when {

      "a valid index is used and the scheme is successfully removed from the session data" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(Map("" -> ""), Map("index" -> "0"))
        val expectedViewModel = sessionData.pensions.shortServiceRefunds.copy(refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithoutUkRefundCharge))

        assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
        getShortServicePensionsViewModel mustBe Some(expectedViewModel)
      }

      "an invalid index is used and there are existing schemes" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(Map("" -> ""), Map("index" -> "4"))

        assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
        getShortServicePensionsViewModel mustBe Some(aShortServiceRefundsViewModel)
      }
      "an invalid index is used and there are no existing schemes" in {
        val sessionData = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(Map("" -> ""), Map("index" -> "4"))

        assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
        getShortServicePensionsViewModel mustBe Some(aShortServiceRefundsEmptySchemeViewModel)
      }
    }

    "redirect to the Pensions Summary page when the user has no stored session data at all" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse = submitForm(Map("" -> ""), Map("index" -> "0"))

      assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      getTransferPensionsViewModel mustBe None
    }
  }
}
