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
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.{AmountForm, No, Yes, NoButHasAgreedToPay}
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


class TaxPaidByPensionProviderAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  val existingAmount: String = "100.88"

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"
    val paragraphSelector: String = "#main-content > div > div > p"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your pension provider pay?"
    val expectedHeading = "How much tax did your pension provider pay?"
    val expectedParagraph = "If more than one of your pension schemes paid the tax, you can add these details later."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter the amount of tax your pension provider paid"
    val invalidFormatErrorText = "Enter the amount of tax your pension provider paid in the correct format"
    val maxAmountErrorText = "The amount of tax your pension provider paid must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your pension provider pay?"
    val expectedHeading = "How much tax did your pension provider pay?"
    val expectedParagraph = "If more than one of your pension schemes paid the tax, you can add these details later."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter the amount of tax your pension provider paid"
    val invalidFormatErrorText = "Enter the amount of tax your pension provider paid in the correct format"
    val maxAmountErrorText = "The amount of tax your pension provider paid must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your client’s pension provider pay?"
    val expectedHeading = "How much tax did your client’s pension provider pay?"
    val expectedParagraph = "If more than one of your client’s pension schemes paid the tax, you can add these details later."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter the amount of tax your client’s pension provider paid"
    val invalidFormatErrorText = "Enter the amount of tax your client’s pension provider paid in the correct format"
    val maxAmountErrorText = "The amount of tax your client’s pension provider paid must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your client’s pension provider pay?"
    val expectedHeading = "How much tax did your client’s pension provider pay?"
    val expectedParagraph = "If more than one of your client’s pension schemes paid the tax, you can add these details later."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter the amount of tax your client’s pension provider paid"
    val invalidFormatErrorText = "Enter the amount of tax your client’s pension provider paid in the correct format"
    val maxAmountErrorText = "The amount of tax your client’s pension provider paid must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))

  ".show" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {

        s"pensionProvidePaidAnnualAllowanceQuestion is '$Yes'" should {
          "render the 'How much tax did your pension provider pay?' page with no value when no cya data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = None,
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render the 'How much tax did your pension provider pay?' page prefilled when cya data" which {

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

              urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }

        s"pensionProvidePaidAnnualAllowanceQuestion is '$No, but has agreed to pay'" should {
          "render the 'How much tax did your pension provider pay?' page with no value when no cya data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = None,
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


              urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render the 'How much tax did your pension provider pay?' page prefilled when cya data" which {

            val existingAmount: String = "999.88"
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

              urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "redirect to the pensionProvidePaidAnnualAllowanceQuestion page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)

        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          pensionProvidePaidAnnualAllowanceQuestion = None, taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
          reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

        urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionProviderPaidTaxUrl(taxYearEOY)) shouldBe true
      }
    }

    s"redirect to the pensionProvidePaidAnnualAllowanceQuestion page if the that question has been answered as '$No'" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          pensionProvidePaidAnnualAllowanceQuestion = Some(No.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
          reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionProviderPaidTaxUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the annual allowance CYA if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      //TODO redirect to annual allowance CYA Page
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {

        s"pensionProvidePaidAnnualAllowanceQuestion is '$Yes'" should {

          "return an error when the form is submitted with no input entry" which {
            val amountEmpty = ""
            val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with an invalid format input" which {

            val amountInvalidFormat = "invalid"
            val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with input over maximum allowed value" which {

            val amountOverMaximum = "100,000,000,000"
            val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(Yes.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)),
                body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.maxAmountErrorText)
            welshToggleCheck(user.isWelsh)
          }
        }

        s"pensionProvidePaidAnnualAllowanceQuestion is '$NoButHasAgreedToPay'" should {

          "return an error when the form is submitted with no input entry" which {
            val amountEmpty = ""
            val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with an invalid format input" which {

            val amountInvalidFormat = "invalid"
            val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with input over maximum allowed value" which {

            val amountOverMaximum = "100,000,000,000"
            val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

            lazy val result: WSResponse = {
              dropPensionsDB()

              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = Some(BigDecimal(existingAmount)),
                reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)


              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)),
                body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(pensionProviderPaidTaxAmountUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.maxAmountErrorText)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "redirect to the pension provider paid tax page when a valid amount is submitted and update the session amount" which {

      val validAmount = "1888.88"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          pensionProvidePaidAnnualAllowanceQuestion = Some(NoButHasAgreedToPay.toString), taxPaidByPensionProvider = None,

          reducedAnnualAllowanceQuestion = Some(true), aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(29.11))

        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionProviderPaidTaxAmountUrl(taxYearEOY)),
          body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: redirect to the annual allowance pension scheme tax reference page when available
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }

      s"updates taxPaidByPensionProvider to Some($validAmount)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe Some(BigDecimal(validAmount))
      }
    }
  }
}
