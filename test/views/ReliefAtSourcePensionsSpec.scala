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
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.ReliefAtSourcePensionsView
import views.ReliefAtSourcePensionsSpec._

// scalastyle:off magic.number
object ReliefAtSourcePensionsSpec {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val h2Selector = "#main-content > div > div > form > div > fieldset > legend"
    val example1TextSelector = "#main-content > div > div > ul > li:nth-child(1)"
    val example2TextSelector = "#main-content > div > div > ul > li:nth-child(2)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedH2: String
    val expectedError: String
    val expectedParagraph: String
    val expectedExample1: String
    val expectedExample2: String
    val expectedPensionProviderText: String
    val expectedCheckProviderText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedHeading: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH2 = "Did you pay into a RAS pension?"
    val expectedError = "Select yes if you paid into a RAS pension"
    val expectedParagraph = "These are pensions you pay into from:"
    val expectedExample1 = "your net income (after tax has been deducted), if you’re employed"
    val expectedExample2 = "your taxable income, if you’re self-employed"
    val expectedPensionProviderText = "Your pension provider then claims tax relief for you."
    val expectedCheckProviderText = "You can check with your pension provider whether this applies to you."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH2 = "A wnaethoch chi dalu i mewn i bensiwn RAS?"
    val expectedError = "Dewiswch ‘Iawn’ os gwnaethoch dalu i mewn i bensiwn RAS"
    val expectedParagraph = "Dyma bensiynau rydych yn talu i mewn iddynt o:"
    val expectedExample1 = "eich incwm net (ar ol i dreth cael ei didynnu), os ydych yn gyflogedig"
    val expectedExample2 = "eich incwm trethadwy, os ydych yn hunangyflogedig"
    val expectedPensionProviderText = "Yna, bydd eich darparwr pensiwn yn hawlio rhyddhad treth ar eich cyfer."
    val expectedCheckProviderText = "Gallwch wirio gyda’ch darparwr pensiwn a yw hyn yn berthnasol i chi."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH2 = "Did your client pay into a RAS pension?"
    val expectedError = "Select yes if your client paid into a RAS pension"
    val expectedParagraph = "These are pensions your client pays into from:"
    val expectedExample1 = "your client’s net income (after tax has been deducted), if they’re employed"
    val expectedExample2 = "your client’s taxable income, if they’re self-employed"
    val expectedPensionProviderText = "Your client’s pension provider then claims tax relief for your client."
    val expectedCheckProviderText = "You can check with your client’s pension provider whether this applies to your client."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH2 = "A wnaeth eich cleient dalu i mewn i bensiwn RAS?"
    val expectedError = "Dewiswch ‘Iawn’ os talodd eich cleient i mewn i bensiwn RAS"
    val expectedParagraph = "Dyma bensiynau y mae eich cleient yn talu i mewn iddynt o’i:"
    val expectedExample1 = "incwm net eich cleient (ar ôl didynnu treth), os yw’n gyflogedig"
    val expectedExample2 = "incwm trethadwy eich cleient, os yw’n hunangyflogedig"
    val expectedPensionProviderText = "Yna, bydd y darparwr pensiwn yn hawlio rhyddhad treth ar gyfer eich cleient."
    val expectedCheckProviderText = "Gallwch wirio gyda’r darparwr pensiwn a yw hyn yn berthnasol i’ch cleient."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle = "Relief at source (RAS) pensions"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = "Relief at source (RAS) pensions"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle = "Pensiynau rhyddhad wrth y ffynhonnell (RAS)"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedHeading = "Pensiynau rhyddhad wrth y ffynhonnell (RAS)"    
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }
}

class ReliefAtSourcePensionsSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean) = new PaymentsIntoPensionFormProvider().reliefAtSourcePensionsForm(isAgent)

  private lazy val underTest = inject[ReliefAtSourcePensionsView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render 'Relief at source (RAS) pensions' page with correct content and no pre-filling" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedH2, h2Selector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, example1TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, example2TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(3))
      }

      "render 'Relief at source (RAS) pensions' page with correct content and yes pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedH2, h2Selector)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, example1TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, example2TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(3))
      }

      "render 'Relief at source (RAS) pensions' page with correct content and no pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedH2, h2Selector)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, example1TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, example2TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(3))

      }

      "render with empty form validation error" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(expectedErrorTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedH2, h2Selector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, example1TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, example2TextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(3))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }

    }
  }

}
// scalastyle:on magic.number
