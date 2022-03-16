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

package controllers.pensions.annualAllowance

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances}
import builders.UserBuilder.aUserRequest
import forms.{PensionProviderPaidTaxQuestionForm, YesNoForm}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{aboveReducedAnnualAllowanceUrl, pensionProviderPaidTaxUrl, reducedAnnualAllowanceTypeUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PensionProviderPaidTaxControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val noButAgreedToPaySelector = "#value-no-agreed-to-pay"
    val findOutLinkSelector = "#annual-allowance-link"
    val overLimitLinkSelector = "#over-limit-link"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($index)"

    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedYouCanFindThisOut: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraph: String
    val expectedNoButAgreedToPayRadio: String
    val yesText: String
    val noText: String
    val expectedButtonText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Has your pension provider paid your annual allowance tax?"
    val expectedHeading = "Has your pension provider paid your annual allowance tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your pension provider paid your annual allowance tax"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Has your pension provider paid your annual allowance tax?"
    val expectedHeading = "Has your pension provider paid your annual allowance tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your pension provider paid your annual allowance tax"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Has your client’s pension provider paid the annual allowance tax?"
    val expectedHeading = "Has your client’s pension provider paid the annual allowance tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client’s pension provider paid the annual allowance tax"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Has your client’s pension provider paid the annual allowance tax?"
    val expectedHeading = "Has your client’s pension provider paid the annual allowance tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client’s pension provider paid the annual allowance tax"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val expectedParagraph = "If more than one pension scheme deals with the tax, you can add their details later."
    val expectedNoButAgreedToPayRadio = "No, but has agreed to pay"
    val expectedButtonText = "Continue"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val expectedParagraph = "If more than one pension scheme deals with the tax, you can add their details later."
    val expectedNoButAgreedToPayRadio = "No, but has agreed to pay"
    val expectedButtonText = "Continue"

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

        "render the 'Pension Provider Paid' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(expectedNoButAgreedToPayRadio, 2, checked = Some(false))
          radioButtonCheck(noText, 3, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }

        "render the 'Pension Provider Paid' page with correct content and yes pre-filled" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some("Yes"))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(expectedNoButAgreedToPayRadio, 2, checked = Some(false))
          radioButtonCheck(noText, 3, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }
        "render the 'Pension Provider Paid' page with correct content and no pre-filled" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some("No"))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(expectedNoButAgreedToPayRadio, 2, checked = Some(false))
          radioButtonCheck(noText, 3, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }
        "render the 'Pension Provider Paid' page with correct content and no, but has agreed to pay pre-filled" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some("NoButHasAgreedToPay"))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(expectedNoButAgreedToPayRadio, 2, checked = Some(true))
          radioButtonCheck(noText, 3, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(expectedNoButAgreedToPayRadio, 2, checked = Some(false))
          radioButtonCheck(noText, 3, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.yesNo -> PensionProviderPaidTaxQuestionForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      //TODO: Redirect to "how much tax did your pension provider pay" amount page
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(Yes)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some("Yes")
      }
    }

    "redirect and update question to 'No' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.yesNo -> PensionProviderPaidTaxQuestionForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(No)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some("No")
      }
    }
    "redirect and update question to 'No, but has agreed to pay' when user selects no, but has agreed to pay when there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.yesNo -> PensionProviderPaidTaxQuestionForm.noHasAgreedToPay)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(No)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some("NoButHasAgreedToPay")
      }
    }
  }
}
// scalastyle:on magic.number
