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
import views.html.pensions.paymentsIntoPensions.WorkplaceAmountView
import views.pensions.paymentsIntoPensions.WorkplaceAmountSpec._

// scalastyle:off magic.number
object WorkplaceAmountSpec {

  val poundPrefixText = "£"
  val amountInputName = "amount"

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val hintTextSelector               = "#amount-hint"
    val poundPrefixSelector            = ".govuk-input__prefix"
    val inputSelector                  = "#amount"
    val expectedErrorHref              = "#amount"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    def insetSpanText(index: Int): String = s"#main-content > div > div > div > span:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val expectedParagraph: String
    val hintText: String
    val buttonText: String

  }

  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedYouCanFindThisOut: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText                       = "For example, £193.52"
    val emptyErrorText                 = "Enter the amount paid into workplace pensions"
    val invalidFormatErrorText         = "Enter the amount paid into workplace pensions in pounds"
    val maxAmountErrorText             = "The amount paid into workplace pensions must be less than £100,000,000,000"
    val buttonText                     = "Continue"
    val expectedParagraph              = "Only include payments:"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText                       = "Er enghraifft, £193.52"
    val emptyErrorText                 = "Nodwch y swm a dalwyd i mewn i bensiynau gweithle"
    val invalidFormatErrorText         = "Nodwch y swm a dalwyd i mewn i bensiynau gweithle yn y fformat cywir"
    val maxAmountErrorText             = "Mae’n rhaid i’r swm a dalwyd i mewn i bensiynau gweithle fod yn llai na £100,000,000,000"
    val buttonText                     = "Yn eich blaen"
    val expectedParagraph              = "Dylech gynnwys y taliadau canlynol yn unig:"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedHeading           = "How much did you pay into your workplace pensions?"
    val expectedTitle             = "How much did you pay into your workplace pensions?"
    val expectedErrorTitle        = s"Error: $expectedTitle"
    val expectedBullet1           = "made after your pay was taxed"
    val expectedBullet2           = "your pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "You can find this out from your employer or your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedHeading           = "Faint y gwnaethoch ei dalu i mewn i bensiynau gweithle?"
    val expectedTitle             = "Faint y gwnaethoch ei dalu i mewn i bensiynau gweithle?"
    val expectedErrorTitle        = s"Gwall: $expectedTitle"
    val expectedBullet1           = "taliadau a wnaed ar ôl i’ch cyflog gael ei drethu"
    val expectedBullet2           = "taliadau na fydd darparwr eich pensiwn yn hawlio rhyddhad treth ar eu cyfer"
    val expectedYouCanFindThisOut = "Gallwch gael gwybod hyn gan eich cyflogwr neu ddarparwr eich pensiwn."

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedHeading           = "How much did your client pay into their workplace pensions?"
    val expectedTitle             = "How much did your client pay into their workplace pensions?"
    val expectedErrorTitle        = s"Error: $expectedTitle"
    val expectedBullet1           = "made after your client’s pay was taxed"
    val expectedBullet2           = "your client’s pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "Your client can find this out from their employer or pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedHeading           = "Faint y gwnaeth eich cleient ei dalu i mewn i bensiynau gweithle?"
    val expectedTitle             = "Faint y gwnaeth eich cleient ei dalu i mewn i bensiynau gweithle?"
    val expectedErrorTitle        = s"Gwall: $expectedTitle"
    val expectedBullet1           = "taliadau a wnaed ar ôl i gyflog eich cleient gael ei drethu"
    val expectedBullet2           = "taliadau na fydd darparwr pensiwn eich cleient yn hawlio rhyddhad treth ar eu cyfer"
    val expectedYouCanFindThisOut = "Gall eich cleient gael gwybod am hyn gan ei gyflogwr neu ddarparwr pensiwn."
  }
}

class WorkplaceAmountSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form = new PaymentsIntoPensionFormProvider().workplacePensionAmountForm

  private lazy val underTest = inject[WorkplaceAmountView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render how much did you pay into your workplace pensions amount page with no pre filling" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form, taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render how much did you pay into your workplace pensions amount page when cya data" which {

        val existingAmount: String                                 = "999.88"
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.fill(999.88), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no input entry" which {

        val amountEmpty                    = ""
        val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(emptyForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
        errorSummaryCheck(emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(emptyErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with an invalid format input" which {

        val amountInvalidFormat                    = "invalid"
        val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(invalidFormatForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
        errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(invalidFormatErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with input over maximum allowed value" which {

        val amountOverMaximum                    = "100,000,000,000"
        val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(overMaximumForm), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
// scalastyle:on magic.number
