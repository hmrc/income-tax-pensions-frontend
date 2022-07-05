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
import views.ReliefAtSourceOneOffPaymentsViewSpec.{CommonExpectedCY, CommonExpectedEN, CommonExpectedResults, ExpectedAgentCY, ExpectedAgentEN, ExpectedIndividualCY, ExpectedIndividualEN, Selectors, SpecificExpectedResults, someRasAmount}
import views.html.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsView

object ReliefAtSourceOneOffPaymentsViewSpec {

  private val someRasAmount: BigDecimal = 33.33

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }


  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val thisIncludes: String
    val expectedErrorMessage: String
  }



  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedHeading = "Did you make any one-off payments into relief at source (RAS) pensions?"
    val expectedTitle = "Did you make any one-off payments into relief at source (RAS) pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val thisIncludes: String =
      s"You told us the total amount you paid plus tax relief was £$someRasAmount. " +
        "Tell us if this includes any one-off payments. A one-off payment is a single payment, made once."
    val expectedErrorMessage = "Select yes if you made one-off payments into RAS pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedHeading = "Did you make any one-off payments into relief at source (RAS) pensions?"
    val expectedTitle = "Did you make any one-off payments into relief at source (RAS) pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val thisIncludes: String =
      s"You told us the total amount you paid plus tax relief was £$someRasAmount. " +
        "Tell us if this includes any one-off payments. A one-off payment is a single payment, made once."
    val expectedErrorMessage = "Select yes if you made one-off payments into RAS pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedHeading = "Did your client make any one-off payments into relief at source (RAS) pensions?"
    val expectedTitle = "Did your client make any one-off payments into relief at source (RAS) pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val thisIncludes: String =
      s"You told us the total amount your client paid plus tax relief was £$someRasAmount. " +
        "Tell us if this includes any one-off payments. A one-off payment is a single payment, made once."
    val expectedErrorMessage = "Select yes if your client made one-off payments into RAS pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedHeading = "Did your client make any one-off payments into relief at source (RAS) pensions?"
    val expectedTitle = "Did your client make any one-off payments into relief at source (RAS) pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val thisIncludes: String =
      s"You told us the total amount your client paid plus tax relief was £$someRasAmount. " +
        "Tell us if this includes any one-off payments. A one-off payment is a single payment, made once."
    val expectedErrorMessage = "Select yes if your client made one-off payments into RAS pensions"
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val paragraphSelector: String = "#this-includes"
  }

}

class ReliefAtSourceOneOffPaymentsViewSpec extends ViewUnitTest {






  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new PaymentsIntoPensionFormProvider().reliefAtSourceOneOffPaymentsForm(isAgent)

  private lazy val underTest = inject[ReliefAtSourceOneOffPaymentsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the one-off payments into relief at source (RAS) pensions question page with no pre-filled radio buttons if no CYA question data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY, ReliefAtSourceOneOffPaymentsViewSpec.someRasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.thisIncludes, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the one-off payments into relief at source (RAS) pensions question page with 'Yes' pre-filled when CYA data exists" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(true), taxYearEOY, someRasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.thisIncludes, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

      }

      "render the one-off payments into relief at source (RAS) pensions question page with 'No' pre-filled when CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(false), taxYearEOY, someRasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.thisIncludes, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(invalidForm), taxYearEOY, someRasAmount)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.thisIncludes, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))

      }
    }

  }

}
