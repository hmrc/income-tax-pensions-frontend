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

package controllers.pensions.unauthorisedPayments

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.{pensionsUserDataWithIncomeFromPensions, pensionsUserDataWithUnauthorisedPayments}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages._
import utils.PageUrls.unauthorisedPaymentsPages.{checkUnauthorisedPaymentsCyaUrl, pensionSchemeTaxReferenceUrl, ukPensionSchemeDetailsUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class UkPensionSchemeDetailsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

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
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Unauthorised payments from UK pensions schemes"
    val expectedHeading = "Unauthorised payments from UK pensions schemes"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another Pensions Scheme Tax Reference"
    val expectedAddPensionSchemeText = "Add a Pensions Scheme Tax Reference"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Unauthorised payments from UK pensions schemes"
    val expectedHeading = "Unauthorised payments from UK pensions schemes"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another Pensions Scheme Tax Reference"
    val expectedAddPensionSchemeText = "Add a Pensions Scheme Tax Reference"
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

        val pensionScheme1 = "12345678RA"
        val pensionScheme2 = "12345678RB"
        val pensionSchemes = Seq(pensionScheme1, pensionScheme2)


        "render the 'UK pension scheme details' summary list page with pre-filled content" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = Some(pensionSchemes))
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)
            urlGet(fullUrl(ukPensionSchemeDetailsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(pensionScheme1, pensionNameSelector(1))
          textOnPageCheck(pensionScheme2, pensionNameSelector(2))
          
          //TODO: replace hrefs "#" below with link to first details page when available .e.g. UkPensionSchemeDetailsCYAController.show(taxYearEOY, Some(1)).url
          linkCheck(s"$change $change $pensionScheme1", changeLinkSelector(1), "#")
          linkCheck(s"$change $change $pensionScheme2", changeLinkSelector(2),"#")
          //TODO: replace hrefs "#" below with link to remove page when available .e.g. RemovePensionSchemeDetailsController.show(taxYearEOY, Some(1)).url
          linkCheck(s"$remove $remove $pensionScheme1", removeLinkSelector(1), "#")
          linkCheck(s"$remove $remove $pensionScheme2", removeLinkSelector(2), "#")
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, pensionSchemeTaxReferenceUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'UK pension scheme details' summary list page with only an add link when there are no income from pensions" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = Some(Seq.empty))
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)

            urlGet(fullUrl(ukPensionSchemeDetailsUrl(taxYearEOY)), user.isWelsh, follow = false,
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
          linkCheck(expectedAddPensionSchemeText, addLinkSelector, pensionSchemeTaxReferenceUrl(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to the income from pensions CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(ukPensionSchemeDetailsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
