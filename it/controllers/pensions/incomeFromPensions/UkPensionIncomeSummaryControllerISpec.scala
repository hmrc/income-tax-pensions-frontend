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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.pensionsUserDataWithIncomeFromPensions
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class UkPensionIncomeSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val addAnotherLinkSelector = "#add-another-pension-link"
    val addLinkSelector = "#add-pension-income-link"
    val continueButtonSelector: String = "#continue"
    val summaryListTableSelector = "#pensionIncomeSummaryList"
    def changeLinkSelector(index: Int): String = s"#pensionIncomeSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__change > a"
    def removeLinkSelector(index: Int): String = s"#pensionIncomeSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__remove > a"
    def pensionNameSelector(index: Int): String = s"#pensionIncomeSummaryList > dl > div:nth-child($index) > dt"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val change: String
    val remove: String
    val expectedButtonText: String
    val expectedAddAnotherText: String
    val expectedAddPensionSchemeText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "UK pension income"
    val expectedHeading = "UK pension income"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedAddPensionSchemeText = "Add a pension scheme"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "UK pension income"
    val expectedHeading = "UK pension income"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedAddPensionSchemeText = "Add a pension scheme"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        val pensionName1 = anUkPensionIncomeViewModelOne.pensionSchemeName.get
        val pensionName2 = anUkPensionIncomeViewModelTwo.pensionSchemeName.get

        "render the 'UK pension income' summary list page with pre-filled content" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(pensionName1, pensionNameSelector(1))
          textOnPageCheck(pensionName2, pensionNameSelector(2))
          
          //TODO: replace hrefs "#" below with link to first details page when available .e.g. UkPensionSchemeDetailsCYAController.show(taxYearEOY, Some(1)).url
          linkCheck(s"$change $change $pensionName1", changeLinkSelector(1), "#")
          linkCheck(s"$change $change $pensionName2", changeLinkSelector(2),"#")
          //TODO: replace hrefs "#" below with link to remove page when available .e.g. RemovePensionSchemeDetailsController.show(taxYearEOY, Some(1)).url
          linkCheck(s"$remove $remove $pensionName1", removeLinkSelector(1), s"${removePensionSchemeUrl(taxYearEOY, Some(0))}")
          linkCheck(s"$remove $remove $pensionName2", removeLinkSelector(2), s"${removePensionSchemeUrl(taxYearEOY, Some(1))}")
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, pensionSchemeDetailsUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(ukPensionIncomeCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'UK pension income' summary list page with only an add link when there are no income from pensions" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq.empty)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)

            urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(summaryListTableSelector)
          linkCheck(expectedAddPensionSchemeText, addLinkSelector, pensionSchemeDetailsUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(ukPensionIncomeCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to the income from pensions CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionIncomeCyaUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
