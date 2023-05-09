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

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import builders.PensionsUserDataBuilder.pensionUserDataWithIncomeOverseasPension
import builders.UserBuilder.aUserRequest
import forms.Countries
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import utils.ViewUtils.bigDecimalCurrency

// scalastyle:off magic.number
class CountrySummaryListControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val urlPrefix = s"/update-and-submit-income-tax-return/pensions/$taxYearEOY/"
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Overseas pension income"
    val expectedHeading = "Overseas pension income"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedAddPensionSchemeText = "Add a pension scheme"
  }
  
  
  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Overseas pension income"
    val expectedHeading = "Overseas pension income"
    val change = "Newid"
    val remove = "Tynnu"
    val expectedAddAnotherText = "Ychwanegu cynllun pensiwn arall"
    val expectedAddPensionSchemeText = "Ychwanegu cynllun pensiwn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  val countries = Countries
  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        val pensionName1 = countries.getCountryFromCodeWithDefault(anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.alphaTwoCode)
        val pensionAmount1 = anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.pensionPaymentAmount
          .fold("")(am => bigDecimalCurrency(am.toString()))
        val pensionName2 = countries.getCountryFromCodeWithDefault(anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(1).alphaTwoCode)
        val pensionAmount2 = anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(1).pensionPaymentAmount
          .fold("")(am => bigDecimalCurrency(am.toString()))

        "render the 'Overseas pension income' summary list page with pre-filled content" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = anIncomeFromOverseasPensionsViewModel
            insertCyaData(pensionUserDataWithIncomeOverseasPension(viewModel), aUserRequest)
            urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(s"$pensionName1 $pensionAmount1", pensionNameSelector(1))
          textOnPageCheck(s"$pensionName2 $pensionAmount2", pensionNameSelector(2))
          
          //TODO: replace hrefs "#" below with link to first details page when available .e.g. PensionSchemeSummaryController.show(taxYear, Some(0))).url
          linkCheck(s"$change $change $pensionName1", changeLinkSelector(1), overseasPensionsSchemeSummaryUrl(taxYearEOY, 0))

          //todo update remove link below when remove functionality is implemented
          linkCheck(s"$remove $remove $pensionName1", removeLinkSelector(1), countrySummaryListControllerUrl(taxYearEOY))

          linkCheck(s"$change $change $pensionName2", changeLinkSelector(2), overseasPensionsSchemeSummaryUrl(taxYearEOY, 1))

          //todo update remove link below when remove functionality is implemented
          linkCheck(s"$remove $remove $pensionName2", removeLinkSelector(2), countrySummaryListControllerUrl(taxYearEOY))

          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, pensionOverseasIncomeCountryUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Overseas pension income' summary list page with only an add link when there are no overseas income from pensions" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val emptyViewModel = anIncomeFromOverseasPensionsEmptyViewModel
            insertCyaData(pensionUserDataWithIncomeOverseasPension(emptyViewModel), aUserRequest)

            urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(summaryListTableSelector)
          linkCheck(expectedAddPensionSchemeText, addLinkSelector, pensionOverseasIncomeCountryUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), follow = false,
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
