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
import forms.{PensionSchemeTaxReferenceForm, YesNoForm}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{aboveReducedAnnualAllowanceUrl, pensionSchemeTaxReferenceUrl, reducedAnnualAllowanceTypeUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PensionSchemeTaxReferenceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val findOutLinkSelector = "#annual-allowance-link"
    val overLimitLinkSelector = "#over-limit-link"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"
    val inputSelector = "#taxReferenceId"
    val hintTextSelector = "#taxReferenceId-hint"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($index)"

    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedNoEntryError: String
    val expectedIncorrectFormatError: String
    val expectedParagraph2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val hintText: String
    val expectedParagraph1: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your PSTR"
    val expectedIncorrectFormatError: String = "Enter your PSTR in the correct format"
    val expectedParagraph2: String = "If more than one of your pension schemes paid the tax, you can add these details later."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your PSTR"
    val expectedIncorrectFormatError: String = "Enter your PSTR in the correct format"
    val expectedParagraph2: String = "If more than one of your pension schemes paid the tax, you can add these details later."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s PSTR"
    val expectedIncorrectFormatError: String = "Enter your client’s PSTR in the correct format"
    val expectedParagraph2: String = "If more than one of your client’s pension schemes paid the tax, you can add these details later."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s PSTR"
    val expectedIncorrectFormatError: String = "Enter your client’s PSTR in the correct format"
    val expectedParagraph2: String = "If more than one of your client’s pension schemes paid the tax, you can add these details later."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Pension Scheme Tax Reference"
    val expectedHeading = "Pension Scheme Tax Reference"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ’12345678RA’"
    val expectedParagraph1 = "Enter the reference for the pension scheme that paid the tax."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Pension Scheme Tax Reference"
    val expectedHeading = "Pension Scheme Tax Reference"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ’12345678RA’"
    val expectedParagraph1 = "Enter the reference for the pension scheme that paid the tax."
  }

  val inputName: String = "taxReferenceId"


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

        "render the 'PSTR' page with correct content and no pre-filling and no PSTR index" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'PSTR' page with correct content with pre-filling and a PSTR index" which {
          val taxSchemeRef = "12345678RB"
          val index = 0

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq(taxSchemeRef)))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, index)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, taxSchemeRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY, index), formSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }
    "redirect to the PSTR summary page when the PSTR is out of bounds" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq("12345678AB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
        urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 2)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
//        TODO REDIRECT TO PSTR SUMMARY PAGE
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }


    "Redirect to the annual allowance CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
//        TODO redirect to annual allowance CYA
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryError)
        }
        s"return $BAD_REQUEST error when incorrect format is submitted" which {
          lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "incorrect-format")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "incorrect-format")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedIncorrectFormatError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedIncorrectFormatError)
        }
      }
    }

    "redirect and update question to contain pension scheme tax reference when list PSTR list is empty" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq.empty))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to pension scheme summary page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.size shouldBe 1
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get.head shouldBe "12345678RA"
      }
    }

    "redirect and update pstr when cya data exists" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq("12345678AB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 0)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: Redirect to the pstr summary page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get.head shouldBe "12345678RA"
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get.size shouldBe 1
      }
    }

    "redirect and update pstr list to contain new pstr when there is an existing pstr list" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq("12345678AB", "12345678AC")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: Redirect to the pstr summary page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get.last shouldBe "12345678RA"
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get.size shouldBe 3
      }
    }

    "redirect to pension summary page when pstr index does not exist" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReference = Some(Seq("12345678AB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 3)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: Redirect to the pstr summary page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference.get should not contain ("12345678RA")
      }
    }

    "redirect to annual allowance CYA page if there is no session data" should {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
//        TODO redirect to Annual Allowances CYA
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }
}
// scalastyle:on magic.number
