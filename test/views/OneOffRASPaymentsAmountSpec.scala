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
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.OneOffRASPaymentsAmountTestSupport.{CommonExpectedCY, CommonExpectedEN, CommonExpectedResults, ExpectedAgentCY, ExpectedAgentEN, ExpectedIndividualCY, ExpectedIndividualEN, SpecificExpectedResults}
import views.html.pensions.paymentsIntoPensions.OneOffRASPaymentsAmountView

// scalastyle:off magic.number

object OneOffRASPaymentsAmountTestSupport {

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

    def insetSpanText(index: Int): String = s"#main-content > div > div > div > span:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedHowToWorkOut: String
    val expectedCalculationHeading: String
    val expectedExampleCalculation: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedYouToldUs: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedTitle = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHowToWorkOut = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100."
    val expectedCalculationHeading = "Example calculation"
    val expectedExampleCalculation = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625."
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief"
    val invalidFormatErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format"
    val maxAmountErrorText = "The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Cyfanswm y taliadau untro i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS), ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val expectedTitle = "Cyfanswm y taliadau untro i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS), ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHowToWorkOut = "Er mwyn ei gyfrifo, rhannwch swm eich taliad untro â 80, a lluoswch y canlyniad â 100."
    val expectedCalculationHeading = "Cyfrifiad enghreifftiol"
    val expectedExampleCalculation = "Gwnaeth Elin daliad untro o £500. £500 wedi’i rannu â 80, a’i luosi â 100 yw £625. Ei hateb yw £625."
    val hintText = "For example, £193.52"
    val emptyErrorText = "Nodwch gyfanswm y taliadau untro a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val invalidFormatErrorText = "Nodwch gyfanswm y taliadau untro a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, yn y fformat cywir"
    val maxAmountErrorText = "Mae’n rhaid i gyfanswm y taliadau untro a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, fod yn llai na £100,000,000,000"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedYouToldUs =
      "Rydych wedi rhoi gwybod i ni mai’r cyfanswm a dalwyd gennych, ynghyd â rhyddhad treth, oedd £189.01. Rhowch wybod i ni faint o hwn oedd yn daliad untro. Rhaid cynnwys rhyddhad treth."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedYouToldUs =
      "Rydych wedi rhoi gwybod i ni mai’r cyfanswm a dalwyd gan eich cleient, ynghyd â rhyddhad treth, oedd £189.01. Rhowch wybod i ni faint o hwn oedd yn daliad untro. Rhaid cynnwys rhyddhad treth."
  }
}

class OneOffRASPaymentsAmountTestSupport extends ViewUnitTest {
  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def amountForm = new PaymentsIntoPensionFormProvider().oneOffRASPaymentsAmountForm

  private lazy val underTest = inject[OneOffRASPaymentsAmountView]
  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render Total one off payments into relief at source (RAS) pensions page with no value when no cya data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val rasAmount: BigDecimal = 189.01

        val htmlFormat = underTest(amountForm, taxYearEOY, rasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import OneOffRASPaymentsAmountTestSupport.Selectors._
        import OneOffRASPaymentsAmountTestSupport._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render Total one off payments into relief at source (RAS) pensions page prefilled when cya data" which {

        val existingAmount: String = "999.88"
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val rasAmount: BigDecimal = 189.01

        val htmlFormat = underTest(amountForm.fill(999.88), taxYearEOY, rasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import OneOffRASPaymentsAmountTestSupport.Selectors._
        import OneOffRASPaymentsAmountTestSupport._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

      }

      "return an error when form is submitted with no input entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val rasAmount: BigDecimal = 189.01
        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "")), taxYearEOY, rasAmount)


        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import OneOffRASPaymentsAmountTestSupport.Selectors._
        import OneOffRASPaymentsAmountTestSupport._
        import userScenario.commonExpectedResults._

        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(emptyErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with an invalid format input" which {


        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val rasAmount: BigDecimal = 189.01
        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "invalid")), taxYearEOY, rasAmount)


        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import OneOffRASPaymentsAmountTestSupport.Selectors._
        import OneOffRASPaymentsAmountTestSupport._
        import userScenario.commonExpectedResults._


        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "invalid")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(invalidFormatErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with input over maximum allowed value" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val rasAmount: BigDecimal = 189.01
        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "100,000,000,000")), taxYearEOY, rasAmount)


        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import OneOffRASPaymentsAmountTestSupport.Selectors._
        import OneOffRASPaymentsAmountTestSupport._
        import userScenario.commonExpectedResults._


        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}

// scalastyle:on magic.number


