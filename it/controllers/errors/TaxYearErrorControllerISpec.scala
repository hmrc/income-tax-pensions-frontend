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

package controllers.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class TaxYearErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }

  val url = s"$appUrl/error/wrong-tax-year"

  trait CommonExpectedResults {
    val h1Expected: String
    val p1Expected: String
    val p1ExpectedSingle: String
    val p2Expected: String
    val p3Expected: String
    val p3ExpectedLink: String
    val p3ExpectedLinkText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "Page not found"
    val p1Expected = s"You can only enter information for the tax years $taxYearEndOfYearMinusOne to $taxYear."
    val p1ExpectedSingle = "You can only enter information for a valid tax year."
    val p2Expected = "Check that you’ve entered the correct web address."
    val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
      "general enquiries (opens in new tab) to speak to someone about your income tax."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "Page not found"
    val p1Expected = s"Dim ond gwybodaeth ar gyfer y blynyddoedd treth $taxYearEndOfYearMinusOne i $taxYear y gallwch ei nodi."
    val p1ExpectedSingle = "Dim ond gwybodaeth ar gyfer blwyddyn dreth ddilys y gallwch ei nodi."
    val p2Expected = "Check that you’ve entered the correct web address."
    val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
      "general enquiries (opens in new tab) to speak to someone about your income tax."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY))
  }

  ".show" when {
    import Selectors._

    val invalidTaxYear: Int = 2050

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with an invalid tax year outside the valid tax year list" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              url,
              welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(invalidTaxYear, validTaxYearList))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          import user.commonExpectedResults._

          titleCheck(h1Expected)
          welshToggleCheck(user.isWelsh)
          h1Check(h1Expected, "xl")
          textOnPageCheck(p1Expected, p1Selector)
          textOnPageCheck(p2Expected, p2Selector)
          textOnPageCheck(p3Expected, p3Selector)
          linkCheck(p3ExpectedLinkText, linkSelector, p3ExpectedLink)
        }

        "render the page with an invalid tax year outside the valid tax year list which contains a single year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              url,
              welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(invalidTaxYear, validTaxYearListSingle))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          import user.commonExpectedResults._

          titleCheck(h1Expected)
          welshToggleCheck(user.isWelsh)
          h1Check(h1Expected, "xl")
          textOnPageCheck(p1ExpectedSingle, p1Selector)
        }
      }
    }
  }
}
