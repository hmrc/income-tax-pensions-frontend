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

package views.pensions.paymentsIntoPensions

import controllers.pensions.paymentsIntoPensions.PaymentsIntoPensionFormProvider
import forms.AmountForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountView
import views.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountSpec._

object ReliefAtSourcePaymentsAndTaxReliefAmountSpec {

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
    val expectedCalculationHeading: String
    val expectedExampleCalculation: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedWhereToFind: String
    val expectedHowToWorkOut: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Total payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedTitle = "Total payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedCalculationHeading = "Example calculation"
    val expectedExampleCalculation = "Emma paid £500 into her pension scheme. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625."
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the total paid into RAS pensions, plus basic rate tax relief"
    val invalidFormatErrorText = "Enter the total paid into RAS pensions, plus basic rate tax relief, in the correct format"
    val maxAmountErrorText = "The total paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedHeading = "Cyfanswm y taliadau i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS), ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val expectedTitle = "Cyfanswm y taliadau i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS), ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedCalculationHeading = "Cyfrifiad enghreifftiol"
    val expectedExampleCalculation = "Talodd Elin £500 i mewn i’w chynllun pensiwn. £500 wedi’i rhannu ag 80, ac yna ei lluosi â 100 yw £625. Ei hateb yw £625."
    val hintText = "Er enghraifft, £193.52"
    val emptyErrorText = "Nodwch y cyfanswm a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol"
    val invalidFormatErrorText = "Nodwch y cyfanswm a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, yn y fformat cywir"
    val maxAmountErrorText = "Mae’n rhaid i’r cyfanswm a dalwyd i mewn i bensiynau RAS, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, fod yn llai na £100,000,000,000"
    val buttonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedWhereToFind =
      "You can find the total amount you paid into RAS pensions, plus tax relief, on the pension certificate or receipt from your administrator."
    val expectedHowToWorkOut =
      "To work it out yourself, divide the amount you actually paid by 80 and multiply the result by 100."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedWhereToFind =
      "Gallwch ddod o hyd i’r cyfanswm a daloch i mewn i bensiynau RAS, ynghyd â rhyddhad treth, ar y dystysgrif pensiwn neu’r dderbynneb gan eich gweinyddwr."
    val expectedHowToWorkOut =
      "Er mwyn cyfrifo hyn eich hun, rhannwch y swm y gwnaethoch ei dalu mewn gwirionedd ag 80, a lluoswch y canlyniad â 100."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedWhereToFind =
      "You can find the total amount your client paid into RAS pensions, plus tax relief, on the pension certificate or receipt from your administrator."
    val expectedHowToWorkOut =
      "To work it out yourself, divide the amount your client actually paid by 80 and multiply the result by 100."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedWhereToFind =
      "Gallwch ddod o hyd i’r cyfanswm a dalwyd gan eich cleient i mewn i bensiynau RAS, ynghyd â rhyddhad treth, ar y dystysgrif pensiwn neu’r dderbynneb gan eich gweinyddwr."
    val expectedHowToWorkOut =
      "Er mwyn cyfrifo hyn eich hun, rhannwch y swm a dalwyd gan eich cleient mewn gwirionedd ag 80, a lluoswch y canlyniad â 100."
  }
}

class ReliefAtSourcePaymentsAndTaxReliefAmountSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def amountForm = new PaymentsIntoPensionFormProvider().reliefAtSourcePaymentsAndTaxReliefAmountForm

  private lazy val underTest = inject[ReliefAtSourcePaymentsAndTaxReliefAmountView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render Total payments into relief at source (RAS) pensions, plus basic rate tax relief page with no value when no cya data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(amountForm, taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render Total payments into relief at source (RAS) pensions, plus basic rate tax relief page prefilled when cya data" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(amountForm.fill(999.98), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "999.98")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

      }

      "return an error when form is submitted with no input entry" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(emptyErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with an invalid format input" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "123.33.33")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "123.33.33")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(invalidFormatErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with input over maximum allowed value" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(amountForm.bind(Map(AmountForm.amount -> "9999999999999999999999999999")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "9999999999999999999999999999")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

    }
  }

}

