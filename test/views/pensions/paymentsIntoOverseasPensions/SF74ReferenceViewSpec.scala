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

package views.pensions.paymentsIntoOverseasPensions

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
    val errorText: String
  }

  object ExpectedContentsIndividualEN extends ExpectedContents {
    val expectedTitle: String = "SF74 reference"
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedPara1: String = "Enter your SF74 reference. You can find this out from your overseas scheme manager."
    val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    val continue: String = "Continue"
    val emptyErrorMessage: String = "Enter your SF74 reference"
    val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
    val errorText: String = "Error: "
  }

  object ExpectedContentsIndividualCY extends ExpectedContents {
    val expectedTitle: String = "Cyfeirnod SF74"
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedPara1: String = "Nodwch eich cyfeirnod SF74. Gallwch ddysgu beth yw hyn oddi wrth eich rheolwr cynllun dramor."
    val hintText: String = "Er enghraifft, “SF74/1234”, “SF74/45865” neu “SF74/123456”"
    val continue: String = "Yn eich blaen"
    val emptyErrorMessage: String = "Nodwch eich cyfeirnod SF74"
    val invalidErrorMessage: String = "Mae’n rhaid i’r cyfeirnod SF74 fod rhwng 1 a 10 digid, a chynnwys rhifau yn unig – dim cymeriadau arbennig"
    val errorText: String = "Gwall: "
  }

  object ExpectedContentsAgentEN extends ExpectedContents {
    val expectedTitle: String = "SF74 reference"
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedPara1: String = "Enter your client’s SF74 reference. You can find this out from your client’s overseas scheme manager."
    val hintText: String = "For example, 'SF74/1234', 'SF74/45865' or 'SF74/123456'"
    val continue: String = "Continue"
    val emptyErrorMessage: String = "Enter your SF74 reference"
    val invalidErrorMessage: String = "The SF74 reference must be between 1 and 10 digits long, contain only numbers and no special characters"
    val errorText: String = "Error: "
  }

  object ExpectedContentsAgentCY extends ExpectedContents {
    val expectedTitle: String = "Cyfeirnod SF74"
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedPara1: String = "Nodwch gyfeirnod SF74 eich cleient. Gallwch ddysgu beth yw hyn oddi wrth reolwr cynllun dramor eich cleient."
    val hintText: String = "Er enghraifft, “SF74/1234”, “SF74/45865” neu “SF74/123456”"
    val continue: String = "Yn eich blaen"
    val emptyErrorMessage: String = "Nodwch eich cyfeirnod SF74"
    val invalidErrorMessage: String = "Mae’n rhaid i’r cyfeirnod SF74 fod rhwng 1 a 10 digid, a chynnwys rhifau yn unig – dim cymeriadau arbennig"
    val errorText: String = "Gwall: "
  }

  protected val userScenarios: Seq[UserScenario[ExpectedContents, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedContentsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedContentsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedContentsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedContentsAgentCY)
  )

  private lazy val underTest = inject[SF74ReferenceView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

       def standardPageElementChecks(outputValue: String = "", errorSummary: String = "")(implicit document : Document) : Unit = {
        titleCheck(errorSummary + userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("sf74ReferenceId", Selectors.sf74ReferenceIdValueSelector, outputValue)
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show the SF74 reference page" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form, taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        standardPageElementChecks()
      }

      "show the SF74 reference value " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")), taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        standardPageElementChecks(outputValue = "1234567")
      }

      "show an error message when user passes empty value " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "")), taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        errorAboveElementCheck(userScenario.commonExpectedResults.emptyErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.emptyErrorMessage, "#sf74ReferenceId")
        standardPageElementChecks(errorSummary = userScenario.commonExpectedResults.errorText)
      }

      "show an error message when user passes more than 10 digits " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "123456789012")), taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        errorAboveElementCheck(userScenario.commonExpectedResults.invalidErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.invalidErrorMessage, "#sf74ReferenceId")
        standardPageElementChecks(outputValue = "123456789012", errorSummary = userScenario.commonExpectedResults.errorText)
      }

      "show an error message when user passes digits other than numbers " which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def form: Form[String] = new FormsProvider().sf74ReferenceIdForm
        val htmlFormat = underTest(form.bind(Map(SF74ReferenceForm.sf74ReferenceId -> "SF74/123456")), taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        errorAboveElementCheck(userScenario.commonExpectedResults.invalidErrorMessage, Some("sf74ReferenceId"))
        errorSummaryCheck(userScenario.commonExpectedResults.invalidErrorMessage, "#sf74ReferenceId")
        standardPageElementChecks(outputValue = "SF74/123456", errorSummary = userScenario.commonExpectedResults.errorText)
      }
    }
  }

}
