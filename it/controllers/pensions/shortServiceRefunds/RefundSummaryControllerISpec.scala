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

import builders.PensionsUserDataBuilder.pensionUserDataWithShortServiceViewModel
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import builders.UserBuilder.aUserRequest
import models.pension.charges.OverseasRefundPensionScheme
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.ShortServiceRefunds.refundSummaryUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class RefundSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {

    "renders the 'short service refund' summary list page " should {
      val pensionScheme = OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Pension Scheme 1"))
      val pensionScheme2 = OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Pension Scheme 2"))
      val newPensionSchemes = Seq(pensionScheme, pensionScheme2)
      val refundViewModel = aShortServiceRefundsViewModel.copy(refundPensionScheme = newPensionSchemes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual()
        dropPensionsDB()
        val viewModel = refundViewModel
        insertCyaData(pensionUserDataWithShortServiceViewModel(viewModel), aUserRequest)
        urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirecting to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
