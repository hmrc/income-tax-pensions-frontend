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

package controllers.pensions.annualAllowance

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.AmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{amountAboveAnnualAllowanceUrl, aboveAnnualAllowanceUrl, pensionProviderPaidTaxUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class AboveReducedAnnualAllowanceAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

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
    val emptyNonReducedErrorText: String
    val invalidFormatNonReducedErrorText: String
    val maxAmountNonReducedErrorText: String
    val hintText: String
    val buttonText: String
    val expectedParagraph: String
  }

  trait SpecificExpectedResults {
    val expectedNonReducedTitle: String
    val expectedNonReducedHeading: String
    val expectedNonReducedErrorTitle: String
    val expectedReducedTitle: String
    val expectedReducedHeading: String
    val expectedReducedErrorTitle: String

    val emptyReducedErrorText: String
    val invalidFormatReducedErrorText: String
    val maxAmountReducedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyNonReducedErrorText = "Enter the amount above the annual allowance"
    val invalidFormatNonReducedErrorText = "Enter the amount above the annual allowance in the correct format"
    val maxAmountNonReducedErrorText = "The amount above the annual allowance must be less than £100,000,000,000"
    val buttonText = "Continue"
    val expectedParagraph = "This is the amount on which tax is due."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyNonReducedErrorText = "Enter the amount above the annual allowance"
    val invalidFormatNonReducedErrorText = "Enter the amount above the annual allowance in the correct format"
    val maxAmountNonReducedErrorText = "The amount above the annual allowance must be less than £100,000,000,000"
    val buttonText = "Continue"
    val expectedParagraph = "This is the amount on which tax is due."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedNonReducedTitle = "How much above your annual allowance are you?"
    val expectedNonReducedHeading = "How much above your annual allowance are you?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedReducedTitle = "How much above your reduced annual allowance are you?"
    val expectedReducedHeading = "How much above your reduced annual allowance are you?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val emptyReducedErrorText = "Enter the amount above your reduced annual allowance"
    val invalidFormatReducedErrorText = "Enter the amount above your reduced annual allowance in the correct format"
    val maxAmountReducedErrorText = "The amount above your reduced annual allowance must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedNonReducedTitle = "How much above your annual allowance are you?"
    val expectedNonReducedHeading = "How much above your annual allowance are you?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedReducedTitle = "How much above your reduced annual allowance are you?"
    val expectedReducedHeading = "How much above your reduced annual allowance are you?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val emptyReducedErrorText = "Enter the amount above your reduced annual allowance"
    val invalidFormatReducedErrorText = "Enter the amount above your reduced annual allowance in the correct format"
    val maxAmountReducedErrorText = "The amount above your reduced annual allowance must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedNonReducedTitle = "How much above your client’s annual allowance are they?"
    val expectedNonReducedHeading = "How much above your client’s annual allowance are they?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedReducedTitle = "How much above your client’s reduced annual allowance are they?"
    val expectedReducedHeading = "How much above your client’s reduced annual allowance are they?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val emptyReducedErrorText = "Enter the amount above your client’s reduced annual allowance"
    val invalidFormatReducedErrorText = "Enter the amount above your client’s reduced annual allowance in the correct format"
    val maxAmountReducedErrorText = "The amount above your client’s reduced annual allowance must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedNonReducedTitle = "How much above your client’s annual allowance are they?"
    val expectedNonReducedHeading = "How much above your client’s annual allowance are they?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedReducedTitle = "How much above your client’s reduced annual allowance are they?"
    val expectedReducedHeading = "How much above your client’s reduced annual allowance are they?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val emptyReducedErrorText = "Enter the amount above your client’s reduced annual allowance"
    val invalidFormatReducedErrorText = "Enter the amount above your client’s reduced annual allowance in the correct format"
    val maxAmountReducedErrorText = "The amount above your client’s reduced annual allowance must be less than £100,000,000,000"
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

        "reducedAnnualAllowanceQuestion is true" should {
          "render How much above your annual allowance are you page with no value when no cya data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedReducedTitle)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
          "render How much above your annual allowance are you page prefilled when cya data" which {

            val existingAmount: String = "999.88"
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(BigDecimal(existingAmount)), reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedReducedTitle)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
        "reducedAnnualAllowanceQuestion is false" should {
          "render How much above your annual allowance are you page with no value when no cya data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedNonReducedTitle)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
          "render How much above your annual allowance are you page prefilled when cya data" which {

            val existingAmount: String = "999.88"
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(BigDecimal(existingAmount)), reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }
            titleCheck(user.specificExpectedResults.get.expectedNonReducedTitle)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "redirect to the above annual allowance question page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = None, aboveAnnualAllowance = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(aboveAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the above annual allowance question page if the that question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(false), aboveAnnualAllowance = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(aboveAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the annual allowance CYA if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      //TODO redirect to annual allowance CYA Page
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to reduced annual allowance page if question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), reducedAnnualAllowanceQuestion = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

        urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {

        "reducedAnnualAllowanceQuestion is true" should {

          "return an error when form is submitted with no input entry" which {
            val amountEmpty = ""
            val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.emptyReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyReducedErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with an invalid format input" which {

            val amountInvalidFormat = "invalid"
            val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.invalidFormatReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatReducedErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with input over maximum allowed value" which {

            val amountOverMaximum = "100,000,000,000"
            val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)),
                body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(user.specificExpectedResults.get.maxAmountReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.maxAmountReducedErrorText)
            welshToggleCheck(user.isWelsh)
          }
        }
        "reducedAnnualAllowanceQuestion is false" should {

          "return an error when form is submitted with no input entry" which {
            val amountEmpty = ""
            val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedNonReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(emptyNonReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(emptyNonReducedErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with an invalid format input" which {

            val amountInvalidFormat = "invalid"
            val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
                follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedNonReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(invalidFormatNonReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(invalidFormatNonReducedErrorText)
            welshToggleCheck(user.isWelsh)
          }

          "return an error when form is submitted with input over maximum allowed value" which {

            val amountOverMaximum = "100,000,000,000"
            val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
                aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None, reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)),
                body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }
            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedNonReducedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(expectedParagraph, paragraphSelector)
            textOnPageCheck(hintText, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
            buttonCheck(buttonText, continueButtonSelector)
            formPostLinkCheck(amountAboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            errorSummaryCheck(maxAmountNonReducedErrorText, expectedErrorHref)
            errorAboveElementCheck(maxAmountNonReducedErrorText)
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
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(amountAboveAnnualAllowanceUrl(taxYearEOY)),
          body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxUrl(taxYearEOY))
      }

      "updates above Annual Allowance amount to Some(1888.88)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowance shouldBe Some(BigDecimal(validAmount))
      }
    }
  }
}
