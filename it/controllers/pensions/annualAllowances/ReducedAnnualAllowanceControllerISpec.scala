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

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances}
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{annualAllowancesCYAUrl, reducedAnnualAllowanceTypeUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class ReducedAnnualAllowanceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val findOutLinkSelector = "#annual-allowance-link"
    val overLimitLinkSelector = "#over-limit-link"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
    def bulletSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"
    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedErrorTitle: String
    val expectedError: String
    val expectedInfo: String
    val expectedWillBeReducedIf: String
    val expectedExample1: String
    val expectedExample2: String
  }
  

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you have a reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you have a reduced annual allowance"
    val expectedInfo = "Annual allowance is the most you can save in your pension pots each year, before you have to pay tax."
    val expectedWillBeReducedIf = "Your annual allowance will be reduced if:"
    val expectedExample1 = "you flexibly access your pension"
    val expectedExample2 = "both your ‘threshold income’ and ‘adjusted income’ are over the limit (opens in new tab)"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Does your client have a reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client has a reduced annual allowance"
    val expectedInfo = "Annual allowance is the most your client can save in their pension pots each year, before they have to pay tax."
    val expectedWillBeReducedIf = "Your client’s annual allowance will be reduced if:"
    val expectedExample1 = "they flexibly access their pension"
    val expectedExample2 = "both their ‘threshold income’ and ‘adjusted income’ are over the limit (opens in new tab)"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A oes gennych lwfans blynyddol wedi’i ostwng?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os oes gennych lwfans blynyddol wedi’i ostwng"
    val expectedInfo = "Lwfans blynyddol yw’r uchafswm y gallwch ei gynilo yn eich cronfeydd pensiwn bob blwyddyn, cyn i chi orfod talu treth."
    val expectedWillBeReducedIf = "Bydd eich lwfans blynyddol yn cael ei ostwng os:"
    val expectedExample1 = "rydych yn cyrchu eich pensiwn yn hyblyg"
    val expectedExample2 = "mae eich ‘incwm trothwy’ yn ogystal â’ch ‘incwm wedi’i addasu’ yn dros y terfyn (yn agor tab newydd)"
  }
  
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A oes gan eich cleient lwfans blynyddol wedi’i ostwng?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os oes gan eich cleient lwfans blynyddol wedi’i ostwng"
    val expectedInfo = "Lwfans blynyddol yw’r uchafswm y gall eich cleient ei gynilo yn ei gronfeydd pensiwn bob blwyddyn, cyn iddo orfod talu treth."
    val expectedWillBeReducedIf = "Bydd lwfans blynyddol eich cleient yn cael ei ostwng os:"
    val expectedExample1 = "mae’n cyrchu ei bensiwn yn hyblyg"
    val expectedExample2 = "mae ei ‘incwm trothwy’ yn ogystal â’i ‘incwm wedi’i addasu’ yn dros y terfyn (yn agor tab newydd)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedFindOut: String
    val expectedFindOutLinkText: String
    val expectedOverLimitLinkText: String
    val expectedDetailsTitle: String
    val expectedDetailsThisIncludes: String
    val expectedDetailsExample1: String
    val expectedDetailsExample2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }
  
  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedFindOut = "Find out what the annual allowance limit is for this tax year (opens in new tab)."
    val expectedFindOutLinkText = "annual allowance limit is for this tax year (opens in new tab)"
    val expectedOverLimitLinkText = "over the limit (opens in new tab)"
    val expectedDetailsTitle = "What does it mean to flexibly access a pension?"
    val expectedDetailsThisIncludes = "This could include taking:"
    val expectedDetailsExample1 = "income from a flexi-access drawdown fund"
    val expectedDetailsExample2 = "cash directly from a pension pot (‘uncrystallised funds pension lump sums’)"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfansau blynyddol ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedFindOut = "Dysgwch beth yw terfyn y lwfans blynyddol ar gyfer y flwyddyn dreth hon (yn agor tab newydd)."
    val expectedFindOutLinkText = "terfyn y lwfans blynyddol ar gyfer y flwyddyn dreth hon (yn agor tab newydd)"
    val expectedOverLimitLinkText = "dros y terfyn (yn agor tab newydd)"
    val expectedDetailsTitle = "Beth y mae’n ei olygu i gyrchu eich pensiwn yn hyblyg?"
    val expectedDetailsThisIncludes = "Gallai hyn gynnwys cymryd:"
    val expectedDetailsExample1 = "incwm a gyrchir yn hyblyg o gronfa"
    val expectedDetailsExample2 = "arian parod yn uniongyrchol o gronfa bensiwn (‘arian heb ei ddefnyddio ar ffurf cyfandaliad pensiwn’)"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'Reduced annual allowance' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya)
            urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfo, paragraphSelector(1))
          textOnPageCheck(expectedFindOut, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWillBeReducedIf, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceUrl(taxYearEOY), formSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsExample1, detailsBulletSelector(1))
          textOnPageCheck(expectedDetailsExample2, detailsBulletSelector(2))
          welshToggleCheck(user.isWelsh)
          linkCheck(expectedFindOutLinkText, findOutLinkSelector,
            "https://www.gov.uk/government/publications/rates-and-allowances-pension-schemes/pension-schemes-rates#annual-allowance")
          linkCheck(expectedOverLimitLinkText, overLimitLinkSelector, "https://www.gov.uk/guidance/pension-schemes-work-out-your-tapered-annual-allowance")
        }

        "render the 'Reduced annual allowance' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(true))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfo, paragraphSelector(1))
          textOnPageCheck(expectedFindOut, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWillBeReducedIf, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceUrl(taxYearEOY), formSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsExample1, detailsBulletSelector(1))
          textOnPageCheck(expectedDetailsExample2, detailsBulletSelector(2))
          welshToggleCheck(user.isWelsh)
          linkCheck(expectedFindOutLinkText, findOutLinkSelector,
            "https://www.gov.uk/government/publications/rates-and-allowances-pension-schemes/pension-schemes-rates#annual-allowance")
          linkCheck(expectedOverLimitLinkText, overLimitLinkSelector, "https://www.gov.uk/guidance/pension-schemes-work-out-your-tapered-annual-allowance")
        }

        "render the 'Reduced annual allowance' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfo, paragraphSelector(1))
          textOnPageCheck(expectedFindOut, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWillBeReducedIf, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceUrl(taxYearEOY), formSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsExample1, detailsBulletSelector(1))
          textOnPageCheck(expectedDetailsExample2, detailsBulletSelector(2))
          welshToggleCheck(user.isWelsh)
          linkCheck(expectedFindOutLinkText, findOutLinkSelector,
            "https://www.gov.uk/government/publications/rates-and-allowances-pension-schemes/pension-schemes-rates#annual-allowance")
          linkCheck(expectedOverLimitLinkText, overLimitLinkSelector, "https://www.gov.uk/guidance/pension-schemes-work-out-your-tapered-annual-allowance")
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfo, paragraphSelector(1))
          textOnPageCheck(expectedFindOut, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWillBeReducedIf, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceUrl(taxYearEOY), formSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsExample1, detailsBulletSelector(1))
          textOnPageCheck(expectedDetailsExample2, detailsBulletSelector(2))
          welshToggleCheck(user.isWelsh)
          linkCheck(expectedFindOutLinkText, findOutLinkSelector,
            "https://www.gov.uk/government/publications/rates-and-allowances-pension-schemes/pension-schemes-rates#annual-allowance")
          linkCheck(expectedOverLimitLinkText, overLimitLinkSelector, "https://www.gov.uk/guidance/pension-schemes-work-out-your-tapered-annual-allowance")
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect to Pensions Summary when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect and update question to 'Yes' when user selects yes and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          reducedAnnualAllowanceQuestion = None, moneyPurchaseAnnualAllowance = None, taperedAnnualAllowance = None)
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(reducedAnnualAllowanceTypeUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          reducedAnnualAllowanceQuestion = Some(true), moneyPurchaseAnnualAllowance = Some(true), taperedAnnualAllowance = Some(true))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion shouldBe Some(false)
      }
    }
  }
}
// scalastyle:on magic.number
