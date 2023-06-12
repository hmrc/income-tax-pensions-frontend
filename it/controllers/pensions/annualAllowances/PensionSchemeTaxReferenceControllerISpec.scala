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
import forms.PensionSchemeTaxReferenceForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{pensionSchemeTaxReferenceUrl, pstrSummaryUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PensionSchemeTaxReferenceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#taxReferenceId"
    val hintTextSelector = "#taxReferenceId-hint"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedNoEntryError: String
    val expectedIncorrectFormatError: String
    val expectedParagraph1: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedErrorTitle: String
    val hintText: String
    val expectedParagraph2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your PSTR"
    val expectedIncorrectFormatError: String = "Enter your PSTR in the correct format"
    val expectedParagraph1: String = "If more than one pension scheme paid or agreed to pay the tax, you can add them later."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Nodwch eich PSTR"
    val expectedIncorrectFormatError: String = "Nodwch eich PSTR yn y fformat cywir"
    val expectedParagraph1: String = "Os gwnaeth mwy nag un o’ch cynlluniau pensiwn dalu’r dreth, gallwch ychwanegu’r manylion hyn yn nes ymlaen."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s PSTR"
    val expectedIncorrectFormatError: String = "Enter your client’s PSTR in the correct format"
    val expectedParagraph1: String = "If more than one of your client’s pension schemes paid the tax, you can add these details later."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Nodwch PSTR eich cleient"
    val expectedIncorrectFormatError: String = "Nodwch PSTR eich cleient yn y fformat cywir"
    val expectedParagraph1: String =
      "Os gwnaeth mwy nag un o gynlluniau pensiwn eich cleient dalu’r dreth, gallwch ychwanegu’r manylion hyn yn nes ymlaen."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Tell us the pension scheme that paid or agreed to pay the tax"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ‘12345678RA’"
    val expectedParagraph2: String = "Pension Scheme Tax Reference"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val expectedTitle = "Rhowch wybod i ni’r cynllun pensiwn a dalodd neu a gytunwyd i dalu’r dreth"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val hintText = "Er enghraifft, ‘12345678RA’"
    val expectedParagraph2: String = "Cyfeirnod Treth y Cynllun Pensiwn"
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
            insertCyaData(anPensionsUserDataEmptyCya)
            urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedParagraph2, paragraphSelector(2))
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
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq(taxSchemeRef)))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, index)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedParagraph2, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, taxSchemeRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY, index), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'PSTR' page with correct content with pre-filling and an invalid PSTR using correct index" which {
          val taxSchemeRef = "1234567RB"
          val index = 0

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq(taxSchemeRef)))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, index)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedParagraph2, paragraphSelector(2))
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
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678AB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 2)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
      }
    }


    "Redirect to the annual allowance CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedParagraph2, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryError)
        }
        s"return $BAD_REQUEST error when incorrect format is submitted" which {
          lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "1234567AB")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedParagraph2, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "1234567AB")
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
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq.empty))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.size shouldBe 1
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get.head shouldBe "12345678RA"
      }
    }

    "redirect and update pstr when cya data exists" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678RB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 0)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get.head shouldBe "12345678RA"
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get.size shouldBe 1
      }
    }

    "redirect and update pstr list to contain new pstr when there is an existing pstr list" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678RB", "12345678RC")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get.last shouldBe "12345678RA"
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get.size shouldBe 3
      }
    }

    "redirect to pension summary page when pstr index does not exist" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678RB")))
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY, 3)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pstrSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get should not contain "12345678RA"
      }
    }

    "redirect to annual allowance CYA page if there is no session data" should {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
