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
import views.html.pensions.incomeFromPensions.StateBenefitsStartDateView

class stateBenefitStartDateViewSpec extends ViewUnitTest with FakeRequestProvider {

  def stateBenefitStartDateUrl(taxYear: Int): String =
    s"/update-and-submit-income-tax-return/pensions/${taxYear.toString}/pension-income/state-pension-start-date"

  private val dayInputName = "stateBenefitStartDate-day"
  private val monthInputName = "stateBenefitStartDate-month"
  private val yearInputName = "stateBenefitStartDate-year"
  private val validDay = "27"
  private val validMonth = "10"
  private val validYear = "2021"


  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintSelector = "#stateBenefitStartDate-hint"
    val dayInputSelector = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector = "#year"

    def labelSelector(index: Int): String = s"#stateBenefitStartDate > div:nth-child($index) > div > label"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedHintText: String
    val expectedDayLabel: String
    val expectedMonthLabel: String
    val expectedYearLabel: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedHintText = "For example, 12 11 2007"
    val expectedDayLabel = "Day"
    val expectedMonthLabel = "Month"
    val expectedYearLabel = "Year"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedHintText = "For example, 12 11 2007"
    val expectedDayLabel = "Diwrnod"
    val expectedMonthLabel = "Mis"
    val expectedYearLabel = "Blwyddyn"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you start getting State Pension payments?"
    val expectedHeading = "When did you start getting State Pension payments?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "When did you start getting State Pension payments?"
    val expectedHeading = "When did you start getting State Pension payments?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client start getting State Pension payments?"
    val expectedHeading = "When did your client start getting State Pension payments?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "When did your client start getting State Pension payments?"
    val expectedHeading = "When did your client start getting State Pension payments?"
  }


  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))

  private lazy val underTest = inject[StateBenefitsStartDateView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(
              incomeFromPensions = anIncomeFromPensionsViewModel.copy(
                statePension = None))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[DateForm.DateModel] = new FormsProvider().stateBenefitDateForm

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "")
        inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "")
        inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "")
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, Selectors.labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, Selectors.labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, Selectors.labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        formPostLinkCheck(stateBenefitStartDateUrl(taxYearEOY), Selectors.formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData,
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[DateForm.DateModel] = new FormsProvider().stateBenefitDateForm

        implicit val document: Document = Jsoup.parse(underTest(form.fill(DateModel(validDay, validMonth, validYear)), taxYearEOY).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
        inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, validMonth)
        inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, validYear)
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, Selectors.labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, Selectors.labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, Selectors.labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
        formPostLinkCheck(stateBenefitStartDateUrl(taxYearEOY), Selectors.formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
    }
}
