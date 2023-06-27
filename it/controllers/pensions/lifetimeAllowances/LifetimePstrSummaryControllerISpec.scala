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

package controllers.pensions.lifetimeAllowances

import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionsUserDataBuilder.pensionsUserDataWithLifetimeAllowance
import controllers.pensions.lifetimeAllowances.routes.PensionSchemeTaxReferenceLifetimeController
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.pensionSchemeTaxReferenceUrl
import utils.PageUrls.PensionLifetimeAllowance.{checkAnnualLifetimeAllowanceCYA, lifetimeAllowancePstrSummaryUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class LifetimePstrSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val pstr1 = "12345678RA"
  val pstr2 = "12345678RB"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val addAnotherLinkSelector = "#add-another-link"
    val addLinkSelector = "#add-pstr-link"
    val continueButtonSelector: String = "#continue"
    def changeLinkSelector(index: Int): String = s"div:nth-child($index) > dd.hmrc-add-to-a-list__change > a"
    def removeLinkSelector(index: Int): String = s"div:nth-child($index) > dd.hmrc-add-to-a-list__remove > a"
    def pstrSelector(index: Int): String = s"#main-content > div > div > div > dl > div:nth-child($index) > dt"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val change: String
    val remove: String
    val pensionSchemeTaxReference: String
    val expectedButtonText: String
    val expectedAddAnotherText: String
    val expectedAddPstrText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Pension schemes that paid or agreed to pay lifetime allowance tax"
    val change = "Change"
    val remove = "Remove"
    val pensionSchemeTaxReference = "Pension Scheme Tax Reference"
    val expectedAddAnotherText = "Add another Pensions Scheme Tax Reference"
    val expectedAddPstrText = "Add a PSTR"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Pension schemes that paid or agreed to pay lifetime allowance tax"
    val change = "Newid"
    val remove = "Tynnu"
    val pensionSchemeTaxReference = "Cyfeirnod Treth y Cynllun Pensiwn"
    val expectedAddAnotherText = "Ychwanegu Cyfeirnod Treth ar gyfer Cynllun Pensiwn arall"
    val expectedAddPstrText = "Ychwanegu PSTR"
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

        "render the 'Lifetime PSTR Summary' page with pre-filled content" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq(pstr1, pstr2)))
            insertCyaData(pensionsUserDataWithLifetimeAllowance(viewModel))
            urlGet(fullUrl(lifetimeAllowancePstrSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(pstr1, pstrSelector(1))
          textOnPageCheck(pstr2, pstrSelector(2))
          linkCheck(s"$change $change $pensionSchemeTaxReference $pstr1", changeLinkSelector(1),
            PensionSchemeTaxReferenceLifetimeController.show(taxYearEOY, Some(0)).url)
          linkCheck(s"$change $change $pensionSchemeTaxReference $pstr2", changeLinkSelector(2),
            PensionSchemeTaxReferenceLifetimeController.show(taxYearEOY, Some(1)).url)
          linkCheck(s"$remove $remove $pensionSchemeTaxReference $pstr1", removeLinkSelector(1),
            "#")
          linkCheck(s"$remove $remove $pensionSchemeTaxReference $pstr2", removeLinkSelector(2),
            "#")
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, PensionSchemeTaxReferenceLifetimeController.show(taxYearEOY, None).url)
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkAnnualLifetimeAllowanceCYA(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Lifetime PSTR Summary' page with only an add link when there are no PSTRs" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq()))
            insertCyaData(pensionsUserDataWithLifetimeAllowance(viewModel))
            urlGet(fullUrl(lifetimeAllowancePstrSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(pstrSelector(1))
          linkCheck(expectedAddPstrText, addLinkSelector, PensionSchemeTaxReferenceLifetimeController.show(taxYearEOY, None).url)
          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkAnnualLifetimeAllowanceCYA(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to the Pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to lifetime allowance cya page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number