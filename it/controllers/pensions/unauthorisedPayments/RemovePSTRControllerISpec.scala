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

package controllers.pensions.unauthorisedPayments

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.fullUrl
import utils.PageUrls.unauthorisedPaymentsPages.{checkUnauthorisedPaymentsCyaUrl, removePensionSchemeReferenceUrl, ukPensionSchemeDetailsUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class RemovePSTRControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String = "#cancel-link-id"
    val insetSpanText: String = "#main-content > div > div > form > div.govuk-inset-text > span"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: Int => String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedHeading = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove reference"
    val cancelText = "Don’t remove"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedHeading = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Remove reference"
    val cancelText = "Don’t remove"
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
            urlGet(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(0))), user.isWelsh, follow = false,
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
          textOnPageCheck("12345678AB", insetSpanText)
          linkCheck(cancelText, cancelLinkSelector, s"${ukPensionSchemeDetailsUrl(taxYearEOY)}")
        }
      }
    }

    "no data is returned" should {

      "redirect to the Unauthorised CYA page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect the user to the Pension Scheme Details page" when {

      "there is no pensionSchemeIndex" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(ukPensionSchemeDetailsUrl(taxYearEOY)) shouldBe true
        }
      }

      "there is an invalid index" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(4))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(ukPensionSchemeDetailsUrl(taxYearEOY)) shouldBe true
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
            authoriseAgentOrIndividual(isAgent = false)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(0))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(ukPensionSchemeDetailsUrl(taxYearEOY)) shouldBe true
          }

          s"remove that scheme from the list" in {
            lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
            cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference shouldBe Some(Seq( "12345678AC"))
          }
        }

        "an invalid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(7))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(ukPensionSchemeDetailsUrl(taxYearEOY)) shouldBe true
          }
        }
      }
    }

    "no data is returned from submission backend" should {

      "redirect to the Unauthorised payments CYA page" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(removePensionSchemeReferenceUrl(taxYearEOY, Some(0))), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
