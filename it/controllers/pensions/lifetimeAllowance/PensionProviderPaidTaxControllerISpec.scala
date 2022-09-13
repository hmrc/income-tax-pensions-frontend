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

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances}
import builders.UserBuilder.aUserRequest
import forms.RadioButtonAmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.pensionSummaryUrl
import utils.CommonUtils
import utils.PageUrls.PensionAnnualAllowancePages.{pensionProviderPaidTaxUrl, pensionSchemeTaxReferenceUrl}
import utils.PageUrls.PensionLifetimeAllowance.pensionAboveAnnualLifetimeAllowanceUrl

// scalastyle:off magic.number
class PensionProviderPaidTaxControllerISpec extends CommonUtils with BeforeAndAfterEach {

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  implicit val url: Int => String = pensionProviderPaidTaxUrl
  private val amountInputName = "amount-2"
  private val existingAmount: Option[BigDecimal] = Some(44.55)
  private val existingAmountStr = "44.55"
  private val amountOverMaximum = "100,000,000,000"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val amountInputSelector = "#amount-2"
    val amountHintSelector = "#amount-2-hint"
    val noButAgreedToPaySelector = "#value-no-agreed-to-pay"
    val amountText = "#conditional-value > div > label"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val noOptionSelectedError: String
    val noEntryErrorMessage: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedLabel: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val hint: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noOptionSelectedError: String = "Select yes if your pension provider paid or agreed to pay your annual allowance tax"
    val noEntryErrorMessage: String = "Enter the amount of tax your pension provider paid or agreed to pay"
    val invalidFormatErrorText: String = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format"
    val maxAmountErrorText: String = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noOptionSelectedError: String = "Select yes if your pension provider paid or agreed to pay your annual allowance tax"
    val noEntryErrorMessage: String = "Enter the amount of tax your pension provider paid or agreed to pay"
    val invalidFormatErrorText: String = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format"
    val maxAmountErrorText: String = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noOptionSelectedError: String = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax"
    val noEntryErrorMessage: String = "Enter the amount of tax your client’s pension provider paid or agreed to pay"
    val invalidFormatErrorText: String = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format"
    val maxAmountErrorText: String = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedHeading = "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noOptionSelectedError: String = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax"
    val noEntryErrorMessage: String = "Enter the amount of tax your client’s pension provider paid or agreed to pay"
    val invalidFormatErrorText: String = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format"
    val maxAmountErrorText: String = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val expectedLabel: String = "Amount they paid or agreed to pay, in pounds"
    val hint: String = "For example, £193.52"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val expectedLabel: String = "Amount they paid or agreed to pay, in pounds"
    val hint: String = "For example, £193.52"
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
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }

        "render the 'Pension Provider Paid' page with correct content and yes pre-filled with a valid amount" which {
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = Some(true),
            taxPaidByPensionProvider = existingAmount)
          lazy val result = showPage(user, pensionsUserDataWithAnnualAllowances(pensionsViewModel))

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLabel, amountText)
          textOnPageCheck(user.commonExpectedResults.hint, amountHintSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          inputFieldValueCheck(amountInputName, amountInputSelector, existingAmountStr)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }
        "render the page with correct content and no pre-filled" which {
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false))
          val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
        }
      }
    }

    "redirect to pensions allowances CYA page if there is no session data" should {
      lazy val result: WSResponse = getResponseNoSessionData

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

        s"return $BAD_REQUEST error when no option is selected" which {
          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")

          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.noOptionSelectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.noOptionSelectedError, Some(RadioButtonAmountForm.yesNo))
        }

        s"return $BAD_REQUEST error when yes is selected but no amount is provided" which {
          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "")

          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLabel, amountText)
          textOnPageCheck(user.commonExpectedResults.hint, amountHintSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.noEntryErrorMessage, amountInputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.noEntryErrorMessage)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
        }
        s"return $BAD_REQUEST error when yes is selected but amount is in incorrect format" which {
          val incorrectFormat = "aa"
          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> incorrectFormat)

          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLabel, amountText)
          textOnPageCheck(user.commonExpectedResults.hint, amountHintSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, Selectors.amountInputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
          inputFieldValueCheck(amountInputName, amountInputSelector, incorrectFormat)
        }

        s"return $BAD_REQUEST error when yes is selected but amount is in excess" which {
          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountOverMaximum)

          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLabel, amountText)
          textOnPageCheck(user.commonExpectedResults.hint, amountHintSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionProviderPaidTaxUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.maxAmountErrorText, Selectors.amountInputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.maxAmountErrorText)
          inputFieldValueCheck(amountInputName, amountInputSelector, amountOverMaximum)
        }
      }
    }
    "redirect and update question to 'Yes' when user selects yes and provides valid amount" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> existingAmountStr)

      lazy val result: WSResponse = submitPage(aPensionsUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxUrl(taxYearEOY))// todo update this when pages required for redirect are updated/implemented
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(Yes)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(true)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe existingAmount
      }
    }

    "redirect and update question to 'No' when user selects no upon entering the page for the first time" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no)

      lazy val result: WSResponse = submitPage(aPensionsUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to CYA when available
        result.header("location") shouldBe Some(pensionProviderPaidTaxUrl(taxYearEOY))// todo update this when pages required for redirect are updated/implemented
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(false)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
      }
    }

    "redirect and update question to 'No' when user selects No when there is cya data and clears the amount" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no)

      val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
        pensionProvidePaidAnnualAllowanceQuestion = Some(true), taxPaidByPensionProvider = existingAmount)

      val pensionUserData = pensionsUserDataWithAnnualAllowances(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to CYA when available
        result.header("location") shouldBe Some(pensionProviderPaidTaxUrl(taxYearEOY))// todo update this when pages required for redirect are updated/implemented
      }

      "updates pensionProvidePaidAnnualAllowanceQuestion to Some(No)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion shouldBe Some(false)
        cyaModel.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
