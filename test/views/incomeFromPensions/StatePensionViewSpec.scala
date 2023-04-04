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

package views.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonAmountForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.incomeFromPensions.StatePensionView

class StatePensionViewSpec extends ViewUnitTest with FakeRequestProvider {

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val amountSubheading: String
    val amountExample: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedNoAmountErrorMessage: String
    val expectedIncorrectFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val amountSubheading = "Total amount this tax year"
    val amountExample = "For example, £193.54"
  }
  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
    val amountSubheading = "Total amount this tax year"
    val amountExample = "Er enghraifft, £193.54"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you get regular State Pension payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if you got State Pension this year"
    val expectedNoAmountErrorMessage = "Enter your State Pension amount"
    val expectedIncorrectFormatErrorMessage = "Enter your State Pension amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your State Pension amount must be less than £100,000,000,000"
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Do you get regular State Pension payments?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if you got State Pension this year"
    val expectedNoAmountErrorMessage = "Enter your State Pension amount"
    val expectedIncorrectFormatErrorMessage = "Enter your State Pension amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your State Pension amount must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Does your client get regular State Pension payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if your client got State Pension this year"
    val expectedNoAmountErrorMessage = "Enter your client’s State Pension amount"
    val expectedIncorrectFormatErrorMessage = "Enter your client’s State Pension amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your client’s State Pension amount must be less than £100,000,000,000"
  }
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Does your client get regular State Pension payments?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if your client got State Pension this year"
    val expectedNoAmountErrorMessage = "Enter your client’s State Pension amount"
    val expectedIncorrectFormatErrorMessage = "Enter your client’s State Pension amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your client’s State Pension amount must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val amountSubheadingSelector: String = "#conditional-value > div > label"
    val amountExampleSelector: String = "#amount-2-hint"
    val amountValueSelector: String = "#amount-2"
  }

  private lazy val underTest = inject[StatePensionView]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, radioNumber = 1, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.amountSubheading, Selectors.amountSubheadingSelector)
        textOnPageCheck(userScenario.commonExpectedResults.amountExample, Selectors.amountExampleSelector)
        inputFieldValueCheck("amount-2", Selectors.amountValueSelector, "")
        radioButtonCheck(userScenario.commonExpectedResults.noText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "42.24")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, radioNumber = 1, checked = true)
        textOnPageCheck(userScenario.commonExpectedResults.amountSubheading, Selectors.amountSubheadingSelector)
        textOnPageCheck(userScenario.commonExpectedResults.amountExample, Selectors.amountExampleSelector)
        inputFieldValueCheck("amount-2", Selectors.amountValueSelector, "42.24")
        radioButtonCheck(userScenario.commonExpectedResults.noText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with no-entry error when no data is submitted" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, "#value")
      }

      "render page with no-entry error when no pension amount is submitted" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoAmountErrorMessage, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoAmountErrorMessage, "#amount-2")
      }

      "render page with incorrect-format error when pension amount has wrong format" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "wrongFormat")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, "#amount-2")
      }

      "render page with over-maximum error when pension amount is greater than £100,000,000,000" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "£100,000,000,042")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, "#amount-2")
      }
    }
  }
}