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
import models.pension.charges.PensionScheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages._
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl, pensionSummaryUrl}
import utils.ViewUtils.bigDecimalCurrency
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class CountrySummaryListControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val urlPrefix = s"/update-and-submit-income-tax-return/pensions/$taxYearEOY/"

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
    val expectedNeedToAddPensionSchemeText: String
    val expectedReturnToOverviewPageText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedContinueButtonText = "Continue"
    val expectedAddSchemeButtonText = "Add a scheme"
    val expectedOverviewButtonText = "Return to overview"
    val expectedTitle = "Overseas pension income"
    val expectedHeading = "Overseas pension income"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedNeedToAddPensionSchemeText = "You need to add one or more pension scheme."
    val expectedReturnToOverviewPageText = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedContinueButtonText = "Yn eich blaen"
    val expectedAddSchemeButtonText = "Add a scheme"
    val expectedOverviewButtonText = "Yn ôl i’r trosolwg"
    val expectedTitle = "Incwm o bensiwn tramor"
    val expectedHeading = "Incwm o bensiwn tramor"
    val change = "Newid"
    val remove = "Tynnu"
    val expectedAddAnotherText = "Ychwanegu cynllun pensiwn arall"
    val expectedNeedToAddPensionSchemeText = "You need to add one or more pension scheme."
    val expectedReturnToOverviewPageText = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  val countries = Countries
  ".show" should {

    "redirect to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" in {
        val invalidJourneyViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false))
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionUserDataWithIncomeOverseasPension(invalidJourneyViewModel))
          urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
      }
      "previous questions are unanswered" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsEmptyViewModel))
          urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
      }
    }

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
            insertCyaData(pensionUserDataWithIncomeOverseasPension(viewModel))
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

          linkCheck(s"$remove $remove $pensionName1", removeLinkSelector(1), removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(0)))

          linkCheck(s"$change $change $pensionName2", changeLinkSelector(2), overseasPensionsSchemeSummaryUrl(taxYearEOY, 1))

          linkCheck(s"$remove $remove $pensionName2", removeLinkSelector(2), removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(1)))

          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, pensionOverseasIncomeCountryUrl(taxYearEOY))
          buttonCheck(expectedContinueButtonText, continueButtonSelector, Some(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the page with the 'Add a scheme' specific format when no scheme data is present" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val emptyViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(true))
            insertCyaData(pensionUserDataWithIncomeOverseasPension(emptyViewModel))

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
          buttonCheck(expectedAddSchemeButtonText, addSchemeButtonSelector, Some(pensionOverseasIncomeCountryUrl(taxYearEOY)))
          textOnPageCheck(expectedNeedToAddPensionSchemeText, needToAddSchemeTextSelector)
          buttonCheck(expectedOverviewButtonText, overviewButtonSelector, Some(overseasPensionsSummaryUrl(taxYearEOY)))
          textOnPageCheck(expectedReturnToOverviewPageText, returnToOverviewTextSelector)
          welshToggleCheck(user.isWelsh)
        }

        "filter out any incomplete schemes and update the session data before rendering the page" which {
          val incompleteViewModel = anIncomeFromOverseasPensionsViewModel.copy(
            overseasIncomePensionSchemes = Seq(
              PensionScheme(
                alphaThreeCode = None,
                alphaTwoCode = Some("FR"),
                pensionPaymentAmount = Some(1999.99),
                pensionPaymentTaxPaid = Some(1999.99),
                specialWithholdingTaxQuestion = None,
                specialWithholdingTaxAmount = Some(1999.99),
                foreignTaxCreditReliefQuestion = Some(true),
                taxableAmount = Some(1999.99)
              ),
              PensionScheme(
                alphaThreeCode = None,
                alphaTwoCode = Some("DE"),
                pensionPaymentAmount = Some(2000.00),
                pensionPaymentTaxPaid = Some(400.00),
                specialWithholdingTaxQuestion = Some(true),
                specialWithholdingTaxAmount = Some(400.00),
                foreignTaxCreditReliefQuestion = Some(true),
                taxableAmount = Some(2000.00)
              ),
              PensionScheme(
                alphaThreeCode = None,
                alphaTwoCode = Some("DE"),
                pensionPaymentAmount = None,
                pensionPaymentTaxPaid = Some(400.00),
                specialWithholdingTaxQuestion = Some(true),
                specialWithholdingTaxAmount = Some(400.00),
                foreignTaxCreditReliefQuestion = Some(true),
                taxableAmount = Some(1000.00)
              )
            )
          )
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionUserDataWithIncomeOverseasPension(incompleteViewModel))
            urlGet(fullUrl(countrySummaryListControllerUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "renders the correct valid schemes" which {
            textOnPageCheck(s"$pensionName2 $pensionAmount2", pensionNameSelector(1))
            linkCheck(s"$change $change $pensionName2", changeLinkSelector(1), overseasPensionsSchemeSummaryUrl(taxYearEOY, 0))
            linkCheck(s"$remove $remove $pensionName2", removeLinkSelector(1), removeOverseasIncomeSchemeControllerUrl(taxYearEOY, Some(0)))
          }

          "removes incomplete schemes from the session data" in {
            val filteredSchemes = incompleteViewModel.copy(
              overseasIncomePensionSchemes = Seq(
                PensionScheme(
                  alphaThreeCode = None,
                  alphaTwoCode = Some("DE"),
                  pensionPaymentAmount = Some(2000.00),
                  pensionPaymentTaxPaid = Some(400.00),
                  specialWithholdingTaxQuestion = Some(true),
                  specialWithholdingTaxAmount = Some(400.00),
                  foreignTaxCreditReliefQuestion = Some(true),
                  taxableAmount = Some(2000.00)
                )
              )
            )
            lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
            cyaModel.pensions.incomeFromOverseasPensions shouldBe filteredSchemes
          }
        }
      }
    }
  }
}
// scalastyle:on magic.number
