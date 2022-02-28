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

package controllers.pensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
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
import utils.PageUrls.{checkPaymentsIntoPensionCyaUrl, fullUrl, retirementAnnuityAmountUrl, retirementAnnuityUrl, workplacePensionUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class RetirementAnnuityAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  private val taxYearEOY: Int = taxYear - 1
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"
    val paragraphSelector: String = "#main-content > div > div > form > div > label > p"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val onlyIncludePayment: String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the amount paid into retirement annuity contracts"
    val invalidFormatErrorText = "Enter the amount paid into retirement annuity contracts in the correct format"
    val maxAmountErrorText = "The amount paid into retirement annuity contracts must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the amount paid into retirement annuity contracts"
    val invalidFormatErrorText = "Enter the amount paid into retirement annuity contracts in the correct format"
    val maxAmountErrorText = "The amount paid into retirement annuity contracts must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val onlyIncludePayment = "Only include payments your pension provider will not claim tax relief for. You can find this out from your pension provider."
    val expectedTitle = "How much did you pay into your retirement annuity contracts?"
    val expectedHeading = "How much did you pay into your retirement annuity contracts?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val onlyIncludePayment = "Only include payments your pension provider will not claim tax relief for. You can find this out from your pension provider."
    val expectedTitle = "How much did you pay into your retirement annuity contracts?"
    val expectedHeading = "How much did you pay into your retirement annuity contracts?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val onlyIncludePayment =
      "Only include payments your client’s pension provider will not claim tax relief for. You can find this out from your client’s pension provider."
    val expectedTitle = "How much did your client pay into their retirement annuity contracts?"
    val expectedHeading = "How much did your client pay into their retirement annuity contracts?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val onlyIncludePayment =
      "Only include payments your client’s pension provider will not claim tax relief for. You can find this out from your client’s pension provider."
    val expectedTitle = "How much did your client pay into their retirement annuity contracts?"
    val expectedHeading = "How much did your client pay into their retirement annuity contracts?"
    val expectedErrorTitle = s"Error: $expectedTitle"
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

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render How much did you pay into your retirement annuity contracts page with no value when no cya data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render How much did you pay into your retirement annuity contracts page prefilled when cya data" which {

          val existingAmount: String = "999.88"
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = Some(BigDecimal(existingAmount)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the retirementAnnuityContractQuestion page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = None, totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(retirementAnnuityUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the retirementAnnuityContractQuestion page if the retirementAnnuityContract question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(false), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(retirementAnnuityUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when form is submitted with no input entry" which {
          val amountEmpty = ""
          val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(emptyErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with an invalid format input" which {

          val amountInvalidFormat = "invalid"
          val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {

          val amountOverMaximum = "100,000,000,000"
          val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)),
              body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the correct page when a valid amount is submitted and update the session amount" which {

      val validAmount = "1888.88"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)),
          body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(workplacePensionUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe Some(BigDecimal(validAmount))
      }
    }
  }
}
