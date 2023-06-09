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

package controllers.pensions.incomeFromOverseasPensions

import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsSingleSchemeViewModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelTwo
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.{countrySummaryListControllerUrl, overseasPensionsSchemeSummaryUrl, removeOverseasIncomeSchemeControllerUrl}
import utils.PageUrls.IncomeFromPensionsPages.{removePensionSchemeUrl, ukPensionIncomeCyaUrl, ukPensionSchemeSummaryListUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class RemoveOverseasIncomeSchemeControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String = "#cancel-link-id"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedCaption: Int => String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove this overseas pension scheme?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove"
    val cancelText = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove this overseas pension scheme?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Tynnu"
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

        "render the Remove Overseas Income Scheme page" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlGet(fullUrl(removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(0))), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          buttonCheck(buttonText)
          linkCheck(cancelText, cancelLinkSelector, s"${countrySummaryListControllerUrl(taxYearEOY)}")
        }
      }
    }

    "redirect the user to the Country Summary List page" when {

      "there is no index" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removeOverseasIncomeSchemeControllerUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(countrySummaryListControllerUrl(taxYearEOY)) shouldBe true
        }
      }

      "there is an invalid index" should {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(3))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(countrySummaryListControllerUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" when {

    "data is returned from submission backend" should {

      "redirect to the Country Summary List page" when {

        "a valid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual()
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(0))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(countrySummaryListControllerUrl(taxYearEOY)) shouldBe true
          }

          s"remove that scheme from the list" in {
            lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
            cyaModel.pensions.incomeFromOverseasPensions shouldBe anIncomeFromOverseasPensionsSingleSchemeViewModel
          }
        }

        "an invalid index is used" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual()
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(7))), body = "", follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          s"has a SEE_OTHER ($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(countrySummaryListControllerUrl(taxYearEOY)) shouldBe true
          }
        }
      }
    }
  }
}
