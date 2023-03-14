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

package views.paymentsIntoOverseasPensions

import forms.{FormsProvider, SF74ReferenceForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoOverseasPensions.SF74ReferenceView

class SF74ReferenceViewSpec extends ViewUnitTest{

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val paragraphSelector: String = "#main-content > div > div > p"
    val hintTextSelector: String = ".govuk-hint"
    val sf74ReferenceIdValueSelector: String = "#sf74ReferenceId"
    val continueButtonSelector: String = "#continue"
  }

  trait ExpectedContents {
    val expectedTitle: String
    val expectedCaption: Int => String
    val expectedPara1: String
    val hintText: String
    val continue: String
    val emptyErrorMessage: String
    val invalidErrorMessage: String
  }

  object ExpectedContentsIndividualEN extends ExpectedContents {
    override val expectedTitle: String = "SF74 reference"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedPara1: String = "Enter your SF74 reference. You can find this out from your overseas scheme manager."
    override val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    override val continue: String = "Continue"
    override val emptyErrorMessage: String = "Enter your SF74 reference"
    override val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
  }

  object ExpectedContentsIndividualCY extends ExpectedContents {
    override val expectedTitle: String = "SF74 reference"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedPara1: String = "Enter your SF74 reference. You can find this out from your overseas scheme manager."
    override val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    override val continue: String = "Continue"
    override val emptyErrorMessage: String = "Enter your SF74 reference"
    override val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
  }

  object ExpectedContentsAgentEN extends ExpectedContents {
    override val expectedTitle: String = "SF74 reference"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedPara1: String = "Enter your client’s SF74 reference. You can find this out from your client’s overseas scheme manager."
    override val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    override val continue: String = "Continue"
    override val emptyErrorMessage: String = "Enter your SF74 reference"
    override val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
  }

  object ExpectedContentsAgentCY extends ExpectedContents {
    override val expectedTitle: String = "SF74 reference"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedPara1: String = "Enter your client’s SF74 reference. You can find this out from your client’s overseas scheme manager."
    override val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    override val continue: String = "Continue"
    override val emptyErrorMessage: String = "Enter your SF74 reference"
    override val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
  }

  override protected val userScenarios: Seq[UserScenario[ExpectedContents, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedContentsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedContentsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedContentsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedContentsAgentCY)
  )

  private lazy val underTest = inject[SF74ReferenceView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "show the SF74 reference page" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm

        val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show the SF74 reference value " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "1234567")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show an error message when user passes empty value " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck("Error: " + userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.emptyErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.emptyErrorMessage, "#sf74ReferenceId")
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show an error message when user passes more than 10 numbers " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "123456789012")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck("Error: " + userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.invalidErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.invalidErrorMessage, "#sf74ReferenceId")
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "123456789012")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show an error message when user passes alphabets " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "SF74123456")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck("Error: " + userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.invalidErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.invalidErrorMessage, "#sf74ReferenceId")
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "SF74123456")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show an error message when user passes special characters " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "123/123456")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck("Error: " + userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.invalidErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.invalidErrorMessage, "#sf74ReferenceId")
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, "123/123456")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

    }
  }

}
