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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances}
import builders.UserBuilder.aUserRequest
import forms.PensionProviderPaidTaxQuestionForm.valueFieldName
import forms.{No, NoButHasAgreedToPay, PensionProviderPaidTaxQuestionForm, Yes, YesNoForm}
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PensionProviderPaidTaxControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  private val existingAmount: Option[BigDecimal] = Some(44.55)
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
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString))
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
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(No.toString))
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
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString))
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

    "redirect to pensions allowances CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(reducedAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO: go to the CYA page
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
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some(valueFieldName))
        }
      }
    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxAmountUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(Yes)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(Yes.toString)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
      }
    }

    "redirect and update question to 'No' when user selects this option' and there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to CYA when available
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(No)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(No.toString)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
      }
    }

    "redirect and update question to 'No, but has agreed to pay' when user selects this option when there is no cya data" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.noHasAgreedToPay)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxAmountUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(NoButHasAgreedToPay)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(NoButHasAgreedToPay.toString)
      }
    }

    "redirect and update question to 'No' when user selects No when there is cya data and clears the amount" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)

        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(77.88),
          pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = existingAmount)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to CYA when available
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(No)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(No.toString)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
      }
    }

    "redirect and update question to 'NoButHasAgreedToPay, when selected by the user without deleting the exising cya data amount" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.noHasAgreedToPay)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(77.88),
          pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = existingAmount)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxAmountUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(NoButHasAgreedToPay)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(NoButHasAgreedToPay.toString)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe existingAmount
      }
    }

    "redirect and update question to 'Yes', when the user selects yes without deleting the exising cya data amount" which {
      lazy val form: Map[String, String] = Map(PensionProviderPaidTaxQuestionForm.valueFieldName -> PensionProviderPaidTaxQuestionForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(77.88),
          pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = existingAmount)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxAmountUrl(taxYearEOY))
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(Yes)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(Yes.toString)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe existingAmount
      }
    }


  }
}
// scalastyle:on magic.number
