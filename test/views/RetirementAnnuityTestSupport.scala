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

import controllers.pensions.paymentsIntoPension.PaymentsIntoPensionFormProvider
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.PayIntoRetirementAnnuityContractView
import views.RetirementAnnuityTestSupport._

object RetirementAnnuityTestSupport {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val detailsSelector: String = s"#main-content > div > div > form > details > summary > span"

    def h3Selector(index: Int): String = s"#main-content > div > div > form > details > div > h3:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"

    def detailsBulletList(index: Int): String = s"#main-content > div > div > form > details > div > ol > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetailsTitle: String
    val expectedDetails: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraphText: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedYouCanFindThisOut: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "What is a retirement annuity contract?"
    val expectedDetails: String =
      "Retirement annuity contracts are a type of pension scheme. " +
        "They were available before 1988 to the self-employed and to workers not offered a workplace pension."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "What is a retirement annuity contract?"
    val expectedDetails: String =
      "Retirement annuity contracts are a type of pension scheme. " +
        "They were available before 1988 to the self-employed and to workers not offered a workplace pension."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you pay into a retirement annuity contract?"
    val expectedHeading = "Did you pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you pay into a retirement annuity contract?"
    val expectedHeading = "Did you pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay into a retirement annuity contract?"
    val expectedHeading = "Did your client pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your client’s pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay into a retirement annuity contract?"
    val expectedHeading = "Did your client pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your client’s pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }
}
class RetirementAnnuityTestSupport extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new PaymentsIntoPensionFormProvider().retirementAnnuityForm(isAgent)

  private lazy val underTest: PayIntoRetirementAnnuityContractView = inject[PayIntoRetirementAnnuityContractView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the retirement annuity contract question page with no pre-filled radio buttons" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
        formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the retirement annuity contract question page with 'Yes' pre-filled when CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
        formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the retirement annuity contract question page with 'No' pre-filled and not a prior submission" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
        formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(2))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
        formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))
      }
    }
  }
}
