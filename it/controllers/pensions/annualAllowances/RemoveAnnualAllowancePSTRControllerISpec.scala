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

package controllers.pensions.annualAllowances

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{pstrSummaryUrl, removePstrUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class RemoveAnnualAllowancePSTRControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  //scalastyle:off magic.number

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String = "#cancel-link-id"
    val insetSpanText: String = "#main-content > div > div > form > div.govuk-inset-text > span"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedCaption: Int => String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove reference"
    val cancelText = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Dileu cyfeirnod"
    val cancelText = "Canslo"
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

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlGet(fullUrl(removePstrUrl(taxYearEOY, 1)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          buttonCheck(buttonText)
          textOnPageCheck("12345678RB", insetSpanText)
          linkCheck(cancelText, cancelLinkSelector, s"${pstrSummaryUrl(taxYearEOY)}")
        }
      }
    }

    "no data is returned" should {

      "redirect to the Annual Allowances PSTR summary page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 0)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"redirect to PSTR Summary page" in {
          result.status shouldBe OK
        }
      }
    }

    "redirect the user to the Pension Scheme Details page" when {

      "there is no pensionSchemeIndex" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 0)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
        }
      }

      "there is an invalid index" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 12)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
        }
      }
    }
  }

  ".submit" when {

    "data is returned from submission backend" should {

      "redirect to the Pension scheme reference details page" when {

        "a valid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual()
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(removePstrUrl(taxYearEOY, 0)), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
          }

          s"remove that scheme from the list" in {
            lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
            cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences shouldBe Some(Seq("12345678RB", "1234567DRD"))
          }
        }

        "an invalid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual()
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(removePstrUrl(taxYearEOY, 12)), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
          }
        }
      }
    }

    "no data is returned from submission backend" should {

      "redirect to the Unauthorised payments CYA page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          urlPost(fullUrl(removePstrUrl(taxYearEOY, 0)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
        }
      }
    }
  }
}

