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

package views.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionEmptyViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.incomeFromPensions.StatePensionAddToCalculationView

class StatePensionAddToCalculationViewSpec extends ViewUnitTest with FakeRequestProvider {

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to add State Pension to your Income Tax calculation?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add State Pension to your Income Tax calculation"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych chi am ychwanegu Pensiwn y Wladwriaeth i’ch cyfrifiad Treth Incwm?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os ydych am ychwanegu Pensiwn y Wladwriaeth i’ch cyfrifiad Treth Incwm"
  }
  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to add State Pension to your client’s Income Tax calculation?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add State Pension to your client’s Income Tax Calculation"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych chi am ychwanegu Pensiwn y Wladwriaeth i gyfrifiad Treth Incwm eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os ydych am ychwanegu Pensiwn y Wladwriaeth i gyfrifiad Treth Incwm eich cleient"
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
    val amountValueSelector: String = "#value"
  }

  private lazy val underTest = inject[StatePensionAddToCalculationView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionEmptyViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        def form: Form[Boolean] = new FormsProvider().statePensionAddToCalculationForm(userScenario.isAgent)

        val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        questionHeadingCheck(userScenario)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.noText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionEmptyViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        def form: Form[Boolean] = new FormsProvider().statePensionAddToCalculationForm(userScenario.isAgent)

        val htmlFormat = underTest(form.bind(Map(RadioButtonForm.value -> "false")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        questionHeadingCheck(userScenario)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.noText, radioNumber = 2, checked = true)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with no-entry error when no data is submitted" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionEmptyViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        def form: Form[Boolean] = new FormsProvider().statePensionAddToCalculationForm(userScenario.isAgent)

        val htmlFormat = underTest(form.bind(Map(RadioButtonForm.value -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, "#value")
      }
    }
  }

  def questionHeadingCheck(userScenario: UserScenario[CommonExpectedResults, SpecificExpectedResults])(implicit document: Document): Unit = {
    captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
    titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
    h1Check(userScenario.specificExpectedResults.get.expectedHeading)
  }
}
