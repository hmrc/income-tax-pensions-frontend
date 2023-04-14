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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.incomeFromPensions.StatePensionLumpSumStartDateView

class StatePensionLumpSumStartDateViewSpec extends ViewUnitTest with FakeRequestProvider {

  private val dayInputName = "statePensionLumpSumStartDate-day"
  private val monthInputName = "statePensionLumpSumStartDate-month"
  private val yearInputName = "statePensionLumpSumStartDate-year"
  private val validDay = "27"
  private val validMonth = "10"
  private val validYear = "2021"

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedDayLabel: String
    val expectedMonthLabel: String
    val expectedYearLabel: String
    val dateInFutureErrorText: String
    val realDateErrorText: String
    val tooLongAgoErrorText: String
    val emptyAllErrorText: String
    val emptyDayErrorText: String
    val emptyDayMonthErrorText: String
    val emptyDayYearErrorText: String
    val emptyMonthErrorText: String
    val emptyMonthYearErrorText: String
    val emptyYearErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedHint: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedDayLabel = "Day"
    val expectedMonthLabel = "Month"
    val expectedYearLabel = "Year"
    val dateInFutureErrorText = "The pension start date must be in the past"
    val realDateErrorText = "The pension start date must be a real date"
    val tooLongAgoErrorText = "The pension start date must be after 1 January 1900"
    val emptyAllErrorText = "Enter the pension start date"
    val emptyDayErrorText = "The pension start date must include a day"
    val emptyDayMonthErrorText = "The pension start date must include a day and month"
    val emptyDayYearErrorText = "The pension start date must include a day and year"
    val emptyMonthErrorText = "The pension start date must include a month"
    val emptyMonthYearErrorText = "The pension start date must include a month and year"
    val emptyYearErrorText = "The pension start date must include a year"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Yn eich blaen"
    val expectedDayLabel = "Diwrnod"
    val expectedMonthLabel = "Mis"
    val expectedYearLabel = "Blwyddyn"
    val dateInFutureErrorText = "The pension start date must be in the past"
    val realDateErrorText = "The pension start date must be a real date"
    val tooLongAgoErrorText = "The pension start date must be after 1 January 1900"
    val emptyAllErrorText = "Enter the pension start date"
    val emptyDayErrorText = "The pension start date must include a day"
    val emptyDayMonthErrorText = "The pension start date must include a day and month"
    val emptyDayYearErrorText = "The pension start date must include a day and year"
    val emptyMonthErrorText = "The pension start date must include a month"
    val emptyMonthYearErrorText = "The pension start date must include a month and year"
    val emptyYearErrorText = "The pension start date must include a year"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you get your State Pension lump sum?"
    val expectedHint = "You can find this in your P60."
    val expectedErrorTitle = s"Error: $expectedTitle"
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "When did you get your State Pension lump sum?"
    val expectedHint = "You can find this in your P60."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client get their State Pension lump sum?"
    val expectedHint = "They can find this on their P60."
    val expectedErrorTitle = s"Error: $expectedTitle"
  }
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "When did your client get their State Pension lump sum?"
    val expectedHint = "They can find this on their P60."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
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
    val formSelector: String = "#main-content > div > div > form"
    val hintSelector = "#statePensionLumpSumStartDate-hint"
    val dayInputSelector = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector = "#year"

    def labelSelector(index: Int): String = s"#statePensionLumpSumStartDate > div:nth-child($index) > div > label"
  }

  def statePensionLumpSumStartDateUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-date"

  private lazy val underTest = inject[StatePensionLumpSumStartDateView]

  userScenarios.foreach { userScenario =>

    import Selectors._
    def form: Form[DateForm.DateModel] = new FormsProvider().statePensionLumpSumStartDateForm

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(
              incomeFromPensions = anIncomeFromPensionsViewModel.copy(
                statePensionLumpSum = None))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        inputFieldValueCheck(dayInputName, dayInputSelector, "")
        inputFieldValueCheck(monthInputName, monthInputSelector, "")
        inputFieldValueCheck(yearInputName, yearInputSelector, "")
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHint, hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, continueButtonSelector)
        formPostLinkCheck(statePensionLumpSumStartDateUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData,
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form.fill(DateModel(validDay, validMonth, validYear)), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
        inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
        inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedHint, hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, continueButtonSelector)
        formPostLinkCheck(statePensionLumpSumStartDateUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
