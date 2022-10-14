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

package controllers.pensions.lifetimeAllowance

import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import forms.PensionSchemeTaxReferenceForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{pensionSchemeTaxReferenceUrl, pstrSummaryUrl}
import utils.PageUrls.PensionLifetimeAllowance.{pensionTaxReferenceNumberLifetimeAllowanceUrl, pensionTaxReferenceNumberLifetimeAllowanceUrlIndex}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{CommonUtils, IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PensionSchemeTaxReferenceLifetimeControllerISpec extends CommonUtils with BeforeAndAfterEach{

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#taxReferenceId"
    val hintTextSelector = "#taxReferenceId-hint"
    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedNoEntryError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val hintText: String
    val expectedParagraph1: String
    val expectedButtonText: String
    val expectedIncorrectFormatError: String
    val expectedSubtitle: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your Pension Scheme Tax Reference"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your Pension Scheme Tax Reference"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s Pension Scheme Tax Reference"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedNoEntryError: String = "Enter your client’s Pension Scheme Tax Reference"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Tell us the pension scheme that paid or agreed to pay the tax"
    val expectedHeading = "Tell us the pension scheme that paid or agreed to pay the tax"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ‘12345678RA’"
    val expectedParagraph1 = "If more than one pension scheme paid or agreed to pay the tax, you can add them later."
    val expectedIncorrectFormatError = "Enter a reference with 8 numbers and 2 letters, such as ‘12345678AB’"
    val expectedSubtitle: String = "Pension Scheme Tax Reference"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedTitle = "Tell us the pension scheme that paid or agreed to pay the tax"
    val expectedHeading = "Tell us the pension scheme that paid or agreed to pay the tax"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, ‘12345678RA’"
    val expectedParagraph1 = "If more than one pension scheme paid or agreed to pay the tax, you can add them later."
    val expectedIncorrectFormatError = "Enter a reference with 8 numbers and 2 letters, such as ‘12345678AB’"
    val expectedSubtitle: String = "Pension Scheme Tax Reference"
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
          implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrl
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)


          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'PSTR' page with correct content with pre-filling and a PSTR index" which {
          val taxSchemeRef = "12345678RB"
          val index = 0
          implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)
          val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq(taxSchemeRef)))
          val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, taxSchemeRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'PSTR' page with correct content with pre-filling and an invalid PSTR using correct index" which {
          val taxSchemeRef = "1234568B"
          val index = 0
          implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)
          val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq(taxSchemeRef)))
          val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, taxSchemeRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }


    }
    "redirect to the PSTR summary page when the PSTR is out of bounds" should {
      val index = 3
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)
      val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678AB")))
      val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)

      lazy val result: WSResponse = showPage(pensionUserData)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))//Todo update this to pstr summary page when implemented
      }
    }


    "Redirect to the lifetime allowance CYA page if there is no session data" should {
      implicit val url: Int=> String = pensionTaxReferenceNumberLifetimeAllowanceUrl
      lazy val result: WSResponse = getResponseNoSessionData


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
//        TODO redirect to lifetime allowance CYA
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "")
          implicit val url: Int=> String = pensionTaxReferenceNumberLifetimeAllowanceUrl
          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryError)
        }
        s"return $BAD_REQUEST error when incorrect format is submitted" which {
          lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678B")
          implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrl
          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "12345678B")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.commonExpectedResults.expectedIncorrectFormatError, inputSelector)
          errorAboveElementCheck(user.commonExpectedResults.expectedIncorrectFormatError)
        }
      }
    }

    "redirect and update question to contain pension scheme tax reference when list PSTR list is empty" which {
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678AB")
      val pensionsViewModel = aPensionLifetimeAllowancesEmptyViewModel.copy(pensionSchemeTaxReferences = Some(Seq.empty))
      val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrl

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.size shouldBe 1
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get.head shouldBe "12345678AB"
      }
    }

    "redirect and update pstr when cya data exists" which {
      val index = 0
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")
      val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678AB")))
      val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)


      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY))//todo redirect to pstr summary page when implemented
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get.head shouldBe "12345678RA"
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get.size shouldBe 1
      }
    }

    "redirect and update pstr list to contain new pstr when there is an existing pstr list" which {
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrl
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")
      val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678AB", "12345678AC")))
      val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)
      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get.last shouldBe "12345678RA"
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get.size shouldBe 3
      }
    }

    "redirect to pension summary page when pstr index does not exist" which {
      val index = 3
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrlIndex(index)
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")
      val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(pensionSchemeTaxReferences = Some(Seq("12345678AB")))
      val pensionUserData = pensionsUserDataWithLifetimeAllowance(pensionsViewModel)
      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get should not contain ("12345678RA")
      }
    }

    "redirect to lifetime allowance CYA page if there is no session data" should {
      implicit val url: Int => String = pensionTaxReferenceNumberLifetimeAllowanceUrl
      lazy val form: Map[String, String] = Map(PensionSchemeTaxReferenceForm.taxReferenceId -> "12345678RA")

      lazy val result: WSResponse = submitPageNoSessionData(form)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
//        TODO redirect to lifetime Allowances CYA

        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }
}
// scalastyle:on magic.number