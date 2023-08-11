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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PensionsUserDataBuilder.pensionsUserDataWithIncomeFromPensions
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
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
    val addSchemeButtonSelector: String = "#AddAScheme"
    val overviewButtonSelector: String = "#ReturnToOverview"
    val summaryListTableSelector = "#pensionIncomeSummaryList"
    val needToAddSchemeTextSelector: String = "#youNeedToAddOneOrMorePensionScheme1"
    val returnToOverviewTextSelector: String = "#youNeedToAddOneOrMorePensionScheme2"


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
    val expectedContinueButtonText: String
    val expectedAddSchemeButtonText: String
    val expectedOverviewButtonText: String
    val expectedAddAnotherText: String
    val expectedReturnToOverviewPageText: String
    val expectedNeedToAddPensionSchemeText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedContinueButtonText = "Continue"
    val expectedAddSchemeButtonText = "Add a scheme"
    val expectedOverviewButtonText = "Return to overview"
    val expectedTitle = "UK pension income"
    val expectedHeading = "UK pension income"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedReturnToOverviewPageText = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
    val expectedNeedToAddPensionSchemeText = "You need to add one or more pension scheme."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedContinueButtonText = "Yn eich blaen"
    val expectedAddSchemeButtonText = "Ychwanegu cynllun"
    val expectedOverviewButtonText = "Yn ôl i’r trosolwg"
    val expectedTitle = "Incwm o bensiynau’r DU"
    val expectedHeading = expectedTitle
    val change = "Newid"
    val remove = "Tynnu"
    val expectedAddAnotherText = "Ychwanegu cynllun pensiwn arall"
    val expectedReturnToOverviewPageText = "Os nad oes gennych gynllun pensiwn i’w ychwanegu, gallwch ddychwelyd i’r trosolwg a dod nôl yn nes ymlaen."
    val expectedNeedToAddPensionSchemeText = "Bydd angen i chi ychwanegu un cynllun pensiwn neu fwy."
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

        "filter incomplete schemes and render the 'UK pension income' summary list page" when {
          "there is pre-filled content" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropPensionsDB()
              val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(
                anUkPensionIncomeViewModelOne.copy(pensionId = None),
                anUkPensionIncomeViewModelOne,
                anUkPensionIncomeViewModelTwo.copy(pensionSchemeRef = None),
                anUkPensionIncomeViewModelTwo))
              insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
              urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has an OK status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(pensionName1, pensionNameSelector(1))
            textOnPageCheck(pensionName2, pensionNameSelector(2))

            linkCheck(s"$change $change $pensionName1", changeLinkSelector(1), pensionSchemeSummaryUrl(taxYearEOY, Some(0)))
            linkCheck(s"$change $change $pensionName2", changeLinkSelector(2), pensionSchemeSummaryUrl(taxYearEOY, Some(1)))
            linkCheck(s"$remove $remove $pensionName1", removeLinkSelector(1), s"${removePensionSchemeUrl(taxYearEOY, Some(0))}")
            linkCheck(s"$remove $remove $pensionName2", removeLinkSelector(2), s"${removePensionSchemeUrl(taxYearEOY, Some(1))}")
            linkCheck(expectedAddAnotherText, addAnotherLinkSelector, pensionSchemeDetailsUrl(taxYearEOY, None))
            buttonCheck(expectedContinueButtonText, continueButtonSelector, Some(ukPensionIncomeCyaUrl(taxYearEOY)))
            welshToggleCheck(user.isWelsh)
          }

          "there are no income from pensions" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropPensionsDB()
              val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(
                anUkPensionIncomeViewModelOne.copy(pensionId = None),
                anUkPensionIncomeViewModelTwo.copy(pensionSchemeRef = None)))
              insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
              urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), user.isWelsh, follow = false,
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

            buttonCheck(expectedAddSchemeButtonText, addSchemeButtonSelector, Some(pensionSchemeDetailsUrl(taxYearEOY, None)))
            textOnPageCheck(expectedNeedToAddPensionSchemeText, needToAddSchemeTextSelector)
            buttonCheck(expectedOverviewButtonText, overviewButtonSelector, Some(pensionIncomeSummaryUrl(taxYearEOY)))
            textOnPageCheck(expectedReturnToOverviewPageText, returnToOverviewTextSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "redirect to the Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" which {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlGet(fullUrl(ukPensionSchemeSummaryListUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }
    }
  }
}
// scalastyle:on magic.number
