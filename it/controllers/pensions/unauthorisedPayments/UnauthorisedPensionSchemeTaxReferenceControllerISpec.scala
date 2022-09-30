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

import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithUnauthorisedPayments}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.PensionSchemeTaxReferenceForm
import org.jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import org.jsoup.Jsoup
import play.api.libs.ws.WSResponse
import utils.PageUrls.unauthorisedPaymentsPages.pensionSchemeTaxReferenceUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class UnauthorisedPensionSchemeTaxReferenceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

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
    val expectedParagraph2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val hintText: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your Pension Scheme Tax Reference"
    val expectedIncorrectFormatError: String = "Enter your Pension Scheme Tax Reference in the correct format"
    val expectedParagraph2: String = "If you got unauthorised payments from more than one UK pension provider, you can add the references later."
    val expectedParagraph1 = "You can get this information from your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your Pension Scheme Tax Reference"
    val expectedIncorrectFormatError: String = "Enter your Pension Scheme Tax Reference in the correct format"
    val expectedParagraph2: String = "If you got unauthorised payments from more than one UK pension provider, you can add the references later."
    val expectedParagraph1 = "You can get this information from your pension provider."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s Pension Scheme Tax Reference"
    val expectedIncorrectFormatError: String = "Enter your client’s Pension Scheme Tax Reference in the correct format"
    val expectedParagraph2: String = "If your client got unauthorised payments from more than UK pension provider, you can add the references later."
    val expectedParagraph1 = "Your client can get this information from their pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s Pension Scheme Tax Reference"
    val expectedIncorrectFormatError: String = "Enter your client’s Pension Scheme Tax Reference in the correct format"
    val expectedParagraph2: String = "If your client got unauthorised payments from more than UK pension provider, you can add the references later."
    val expectedParagraph1 = "Your client can get this information from their pension provider."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Pension Scheme Tax Reference (PSTR)"
    val expectedHeading = "Pension Scheme Tax Reference (PSTR)"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ‘12345678RA’"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Pension Scheme Tax Reference (PSTR)"
    val expectedHeading = "Pension Scheme Tax Reference (PSTR)"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ‘12345678RA’"
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
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeTaxReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect to the annual allowance CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
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
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(2))
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
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, paragraphSelector(2))
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

    "redirect and add pstr to existing list of pstr even when the pstr is already present in the model " which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RB")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = Some(Seq("12345678RB", "12345678RA")))
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.length shouldBe 3
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.count(x => x == "12345678RB") shouldBe 2
      }
    }

    "redirect and add pstr to existing list of pstr" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = Some(Seq.empty))
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.sorted shouldBe Seq("12345678RA")
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.size shouldBe 1
      }
    }

    "redirect and update pstr list to contain new pstr when the pstr list is empty" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(pensionSchemeTaxReference = Some(Seq.empty))
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(pensionSchemeTaxReferenceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.last shouldBe "12345678RA"
        cyaModel.pensions.unauthorisedPayments.pensionSchemeTaxReference.get.size shouldBe 1
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
