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

package views

import controllers.pensions.paymentsIntoPensions.PaymentsIntoPensionFormProvider
import forms.AmountForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.RetirementAnnuityAmountView
import views.RetirementAnnuityAmountSpec._


object RetirementAnnuityAmountSpec {
  val poundPrefixText = "£"
  val amountInputName = "amount"

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
    val emptyErrorText = "Nodwch y swm a dalwyd i mewn i gontractau blwydd-dal ymddeol"
    val invalidFormatErrorText = "Nodwch y swm a dalwyd i mewn i gontractau blwydd-dal ymddeol yn y fformat cywir"
    val maxAmountErrorText = "Mae’n rhaid i’r swm a dalwyd i mewn i gontractau blwydd-dal ymddeol fod yn llai na £100,000,000,000"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val onlyIncludePayment = "Only include payments your pension provider will not claim tax relief for. You can find this out from your pension provider."
    val expectedTitle = "How much did you pay into your retirement annuity contracts?"
    val expectedHeading = "How much did you pay into your retirement annuity contracts?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val onlyIncludePayment = "Dim ond taliadau na fydd darparwr eich pensiwn yn hawlio rhyddhad treth ar eu cyfer y dylech eu cynnwys. Gallwch ddysgu beth yw hyn oddi wrth ddarparwr eich pensiwn."
    val expectedTitle = "Faint y gwnaethoch ei dalu i mewn i’ch contractau blwydd-dal ymddeol?"
    val expectedHeading = "Faint y gwnaethoch ei dalu i mewn i’ch contractau blwydd-dal ymddeol?"
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
      "Dim ond taliadau na fydd darparwr pensiwn eich cleient yn hawlio rhyddhad treth ar eu cyfer y dylech eu cynnwys. Gallwch ddysgu beth yw hyn oddi wrth ddarparwr pensiwn eich cleient."
    val expectedTitle = "Faint y gwnaeth eich cleient ei dalu i mewn i’w gontractau blwydd-dal ymddeol?"
    val expectedHeading = "Faint y gwnaeth eich cleient ei dalu i mewn i’w gontractau blwydd-dal ymddeol?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }
}

class RetirementAnnuityAmountSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))

  private def form: Form[BigDecimal] = new PaymentsIntoPensionFormProvider().retirementAnnuityAmountForm

  private lazy val underTest = inject[RetirementAnnuityAmountView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render How much did you pay into your retirement annuity contracts page with no value when no cya data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form, taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render How much did you pay into your retirement annuity contracts page prefilled when cya data" which {

        val existingAmount: String = "999.88"

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.fill(999.88), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no input entry" which {
        val emptyForm: Map[String, String] = Map(AmountForm.amount -> "")

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val htmlFormat = underTest(form.bind(emptyForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(emptyErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with an invalid format input" which {
        val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> "invalid")

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(invalidFormatForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "invalid")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(invalidFormatErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with input over maximum allowed value" which {

        val amountOverMaximum = "100,000,000,000"
        val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(overMaximumForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.onlyIncludePayment, paragraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
