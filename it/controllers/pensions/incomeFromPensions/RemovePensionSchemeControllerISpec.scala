/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.incomeFromPensions

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.PageUrls.IncomeFromPensionsPages.{removePensionSchemeUrl, ukPensionSchemeSummaryListUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class RemovePensionSchemeControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String = "#cancel-link-id"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: Int => String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove pension name 1?"
    val expectedHeading = "Are you sure you want to remove pension name 1?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove pension"
    val cancelText = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove pension name 1?"
    val expectedHeading = "Are you sure you want to remove pension name 1?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove pension"
    val cancelText = "Cancel"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None)
  )

  ".show" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)}" should {

        "render the Remove Pension Scheme page" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlGet(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(0))), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          buttonCheck(buttonText)
          linkCheck(cancelText, cancelLinkSelector, s"${ukPensionSchemeSummaryListUrl(taxYearEOY)}")
        }
      }
    }

    "no data is returned" should {

      "redirect to the Pensions Summary page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect the user to the UK Pension Income Summary page" when {

      "there is no pensionSchemeIndex" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(3))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
        }
      }

      "there is an invalid pensionSchemeIndex" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removePensionSchemeUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" when {

    "data is returned from submission backend" should {

      "redirect to the UK Pension Income Summary page" when {

        "a valid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(0))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
          }
        }

        "an invalid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(7))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
          }
        }
      }
    }

    "no data is returned from submission backend" should {

      "redirect to the pensions summary page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(removePensionSchemeUrl(taxYearEOY, Some(0))), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
