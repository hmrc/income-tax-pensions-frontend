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

import builders.OverseasRefundPensionSchemeBuilder.{anOverseasRefundPensionSchemeWithUkRefundCharge, anOverseasRefundPensionSchemeWithoutUkRefundCharge}
import builders.PensionsUserDataBuilder.pensionUserDataWithShortServiceViewModel
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, emptyShortServiceRefundsViewModel}
import builders.UserBuilder.aUserRequest
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.ShortServiceRefunds.{refundSummaryUrl, shortServiceTaxableRefundUrl, taxOnShortServiceRefund}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class RefundSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {

    "render the 'short service refund' summary list page" which {
      val incompleteViewModel = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(
        anOverseasRefundPensionSchemeWithUkRefundCharge, anOverseasRefundPensionSchemeWithoutUkRefundCharge.copy(providerAddress = None)
      ))
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual()
        dropPensionsDB()
        insertCyaData(pensionUserDataWithShortServiceViewModel(incompleteViewModel))
        urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "returns an OK status" in {
        result.status shouldBe OK
      }
      "filters out any incomplete schemes and updates the session data" in {
        val filteredSchemes = incompleteViewModel.copy(refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithUkRefundCharge))
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get

        cyaModel.pensions.shortServiceRefunds shouldBe filteredSchemes
      }
    }

    "redirect to the pensions summary page if there is no session data" in {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))

    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" in {
        val invalidJourneyViewModel = emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false))
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionUserDataWithShortServiceViewModel(invalidJourneyViewModel))
          urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(shortServiceTaxableRefundUrl(taxYearEOY))
      }
      "previous questions are unanswered" in {
        val incompleteViewModel = emptyShortServiceRefundsViewModel.copy(
          shortServiceRefundTaxPaid = Some(true), shortServiceRefundTaxPaidCharge = Some(1000.00)
        )
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionUserDataWithShortServiceViewModel(incompleteViewModel))
          urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(shortServiceTaxableRefundUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
