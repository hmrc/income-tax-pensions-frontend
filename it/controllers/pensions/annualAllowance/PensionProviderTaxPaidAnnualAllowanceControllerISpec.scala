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
import builders.PensionsUserDataBuilder.{anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances}
import builders.UserBuilder.aUserRequest
import forms.RadioButtonAmountForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.libs.ws.WSResponse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import utils.PageUrls.pensionSummaryUrl
import utils.PageUrls.PensionAnnualAllowancePages.pensionProviderTaxPaidAnnualAllowanceUrl
import utils.CommonUtils


class PensionProviderTaxPaidAnnualAllowanceControllerISpec extends CommonUtils with BeforeAndAfterEach {

  private val poundPrefixText = "£"
  private val amountInputName = "amount-2"
  private val zeroAmount = "0"
  private val amountOverMaximum = "100,000,000,000"
  private val existingAmount: String = "200"

  object Selectors {
    val yesSelector = "#value"
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amountInputSelector = "#amount-2"
    val expectedAmountErrorHref = "#amount-2"
    val poundPrefixSelector = ".govuk-input__prefix"
    val amountText = "#conditional-value > div > label"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val totalNonUkTax: String
    val totalNonUkTaxErrorNoEntry: String
    val totalNonUkTaxErrorIncorrectFormat: String
    val totalNonUkTaxErrorOverMaximum: String
    val amountZeroErrorMessage: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val amountAboveLimitErrorMessage: String
    val validAmountErrorMessage: String
    val noEntryErrorMessage: String
    val noEntryAmountErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalNonUkTax: String = "Amount they paid or agreed to pay, in pounds"
    val totalNonUkTaxErrorNoEntry: String = "Enter the amount of non-UK tax paid"
    val totalNonUkTaxErrorIncorrectFormat: String = "Enter the amount of non-UK tax in the correct format"
    val totalNonUkTaxErrorOverMaximum: String = "The amount of non-UK tax paid must be less than £100,000,000,000"
    val amountZeroErrorMessage = "Enter an amount greater than zero"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalNonUkTax: String = "Amount they paid or agreed to pay, in pounds"
    val totalNonUkTaxErrorNoEntry: String = "Enter the amount of non-UK tax paid"
    val totalNonUkTaxErrorIncorrectFormat: String = "Enter the amount of non-UK tax in the correct format"
    val totalNonUkTaxErrorOverMaximum: String = "The amount of non-UK tax paid must be less than £100,000,000,000"
    val amountZeroErrorMessage = "Enter an amount greater than zero"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val amountAboveLimitErrorMessage = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000"
    val validAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in the correct format"
    val noEntryErrorMessage = "Select yes if your pension provider paid or agreed to pay your annual allowance tax"
    val noEntryAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in pounds"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val amountAboveLimitErrorMessage = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000"
    val validAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in the correct format"
    val noEntryErrorMessage = "Select yes if your pension provider paid or agreed to pay your annual allowance tax"
    val noEntryAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in pounds"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val amountAboveLimitErrorMessage = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000"
    val validAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in the correct format"
    val noEntryErrorMessage = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax"
    val noEntryAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in pounds"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val amountAboveLimitErrorMessage = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000"
    val validAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in the correct format"
    val noEntryErrorMessage = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax"
    val noEntryAmountErrorMessage = "Enter the amount paid, or that needs to be paid, in pounds"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private implicit val url: Int => String = pensionProviderTaxPaidAnnualAllowanceUrl

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render page with no pre-filling" which {
          lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render page with 'yes' pre-filled and amount field set to a valid amount" which {

          val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = Some(true),
            taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
          val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

          lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, existingAmount)
          formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render page with 'no' pre-filled and no amount set" which {

          val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = Some(false),
            taxPaidByPensionProvider = None)
          val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

          lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render page with 'yes' pre-filled and amount field set to 0" which {

          val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = Some(true),
            taxPaidByPensionProvider = Some(0))
          val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

          lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, zeroAmount)
          formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }


      }

    }
    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = getResponseNoSessionData

      //TODO - redirect to unauthorised payments CYA page once implemented
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {

      userScenarios.foreach { user =>
        import Selectors._
        import user.commonExpectedResults._
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {


          "redirect to the same page when user selects 'No' and update amount to None" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no)
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(controllers.pensions.annualAllowance.routes.PensionProviderTaxPaidAnnualAllowanceController.show(taxYearEOY).url)
            }

            "updates did you non uk tax on the amount that resulted in a surcharge page to  Some(false) with amount set to 0" in {
              lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
              cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(false)
              cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
            }
          }

          "redirect to PensionProviderPaidTaxController page when user selects 'yes' with a valid amount" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> existingAmount)
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(false),
              taxPaidByPensionProvider = None)
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(controllers.pensions.lifetimeAllowance.routes.PensionProviderPaidTaxController.show(taxYearEOY).url)
            }

            "updates did you non uk tax on the amount that resulted in a surcharge page to  Some(true) with valid amount" in {
              lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
              cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(true)
              cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe Some(BigDecimal(existingAmount))
            }
          }

          "returns error message when user does not enter yes or no and click continue button" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            inputFieldValueCheck(amountInputName, amountInputSelector, "")
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            textOnPageCheck(totalNonUkTax, amountText)
            errorAboveElementCheck(user.specificExpectedResults.get.noEntryErrorMessage)
            formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          }

          "returns error message when user enters yes and enters a blank amount click continue button" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "")
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, amountInputSelector, "")
            textOnPageCheck(totalNonUkTax, amountText)
            errorAboveElementCheck(user.specificExpectedResults.get.noEntryAmountErrorMessage)
            formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          }

          "returns error message when user enters yes and enter 0 for amount and click continue button" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "0")
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, amountInputSelector, zeroAmount)
            textOnPageCheck(totalNonUkTax, amountText)
            errorAboveElementCheck(amountZeroErrorMessage)
            formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          }

          "returns error message when user enters above the maximum amount and clicks continue button" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountOverMaximum)
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck(amountInputName, amountInputSelector, amountOverMaximum)
            textOnPageCheck(totalNonUkTax, amountText)
            errorAboveElementCheck(user.specificExpectedResults.get.amountAboveLimitErrorMessage)
            formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          }

          "returns error message when user enters invalid input for amount and click continue button" which {

            lazy val form: Map[String, String] = Map(
              RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "INVALID")
            val pensionAnnualAllowanceViewModel = aPensionAnnualAllowanceViewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(true),
              taxPaidByPensionProvider = Some(BigDecimal(existingAmount)))
            val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionAnnualAllowanceViewModel, isPriorSubmission = false)

            lazy val result: WSResponse = submitPage(user, pensionUserData, form)

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            textOnPageCheck(totalNonUkTax, amountText)
            inputFieldValueCheck(amountInputName, amountInputSelector, "INVALID")
            errorAboveElementCheck(user.specificExpectedResults.get.validAmountErrorMessage)
            formPostLinkCheck(pensionProviderTaxPaidAnnualAllowanceUrl(taxYearEOY), formSelector)
          }
        }
      }
    }
}
