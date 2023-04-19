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
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
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
import views.html.pensions.incomeFromPensions.PensionSchemeStartDateView

class PensionSchemeStartDateViewSpec extends ViewUnitTest with FakeRequestProvider {

  private val dayInputName = "pensionStartDate-day"
  private val monthInputName = "pensionStartDate-month"
  private val yearInputName = "pensionStartDate-year"
  private val validDay = "27"
  private val validMonth = "10"
  private val validYear = "2021"

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
    lazy val expectedHeading: String = expectedTitle
    val expectedErrorTitle: String
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Yn eich blaen"
    val expectedHintText = "Er enghraifft, 12 11 2007"
    val expectedDayLabel = "Diwrnod"
    val expectedMonthLabel = "Mis"
    val expectedYearLabel = "Blwyddyn"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd y gwnaethoch ddechrau cael taliadau o’r cynllun hwn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd y gwnaeth eich cleient ddechrau cael taliadau o’r cynllun hwn?"
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
    val hintSelector = "#pensionStartDate-hint"
    val dayInputSelector = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector = "#year"
    val dayInputHref = "#day"
    val monthInputHref = "#month"
    val yearInputHref = "#year"

    def labelSelector(index: Int): String = s"#pensionStartDate > div:nth-child($index) > div > label"
  }

  def pensionStartDateUrl(taxYear: Int, pensionSchemeIndex: Int): String =
    s"$appUrl/$taxYear/pension-income/pension-start-date?pensionSchemeIndex=$pensionSchemeIndex"

  private lazy val underTest = inject[PensionSchemeStartDateView]

  userScenarios.foreach { userScenario =>

    import Selectors._
    def form: Form[DateForm.DateModel] = new FormsProvider().pensionSchemeDateForm

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render page with no pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(
              incomeFromPensions = anIncomeFromPensionsViewModel.copy(
                uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = None)))
            )),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form, taxYearEOY, 0)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        inputFieldValueCheck(dayInputName, dayInputSelector, "")
        inputFieldValueCheck(monthInputName, monthInputSelector, "")
        inputFieldValueCheck(yearInputName, yearInputSelector, "")
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, continueButtonSelector)
        formPostLinkCheck(pensionStartDateUrl(taxYearEOY, 0), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(
              incomeFromPensions = anIncomeFromPensionsViewModel.copy(
                uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = Some(s"$validYear-$validMonth-$validDay"))))
            )),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form.fill(DateModel(validDay, validMonth, validYear)), taxYearEOY, 1)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
        inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
        inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, hintSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDayLabel, labelSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedMonthLabel, labelSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedYearLabel, labelSelector(3))
        buttonCheck(userScenario.commonExpectedResults.buttonText, continueButtonSelector)
        formPostLinkCheck(pensionStartDateUrl(taxYearEOY, 1), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
