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
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class RefundSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val urlPrefix = s"/update-and-submit-income-tax-return/pensions/$taxYearEOY/"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty


  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {


        "render the 'short service refund' summary list page " which {
          val pensionScheme = OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Pension Scheme 1"))
          val pensionScheme2 = OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Pension Scheme 2"))
          val newPensionSchemes = Seq(pensionScheme, pensionScheme2)
          val refundViewModel = aShortServiceRefundsViewModel.copy(refundPensionScheme = newPensionSchemes)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = refundViewModel
            insertCyaData(pensionUserDataWithShortServiceViewModel(viewModel), aUserRequest)
            urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }
        }
      }
    }

    "redirect to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(refundSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
