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
import forms.standard.LocalDateFormProvider
import forms.standard.StandardErrorKeys.{EarliestDate, PresentDate}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.incomeFromPensions.StatePensionLumpSumStartDateView

import java.time.LocalDate

class StatePensionLumpSumStartDateViewSpec extends ViewUnitTest with FakeRequestProvider {

  private val dayInputName   = "statePensionLumpSumStartDate-day"
  private val monthInputName = "statePensionLumpSumStartDate-month"
  private val yearInputName  = "statePensionLumpSumStartDate-year"
  private val validDay       = "27"
  private val validMonth     = "10"
  private val validYear      = "2021"
  private val validDate      = LocalDate.parse(s"$validYear-$validMonth-$validDay")
  private val formProvider   = new LocalDateFormProvider()

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedDayLabel: String
    val expectedMonthLabel: String
    val expectedYearLabel: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedHint: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText                     = "Continue"
    val expectedDayLabel               = "Day"
    val expectedMonthLabel             = "Month"
    val expectedYearLabel              = "Year"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText                     = "Yn eich blaen"
    val expectedDayLabel               = "Diwrnod"
    val expectedMonthLabel             = "Mis"
    val expectedYearLabel              = "Blwyddyn"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you get your State Pension lump sum?"
    val expectedHint  = "You can find this in your P60."
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd cawsoch eich cyfandaliad Pensiwn y Wladwriaeth?"
    val expectedHint  = "Mae hwn i’w weld ar eich P60."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client get their State Pension lump sum?"
    val expectedHint  = "They can find this on their P60."
  }
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd cafodd eich cleient gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedHint  = "Mae hwn i’w weld ar ei P60."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val hintSelector                   = "#statePensionLumpSumStartDate-hint"
    val dayInputSelector               = "#day"
    val monthInputSelector             = "#month"
    val yearInputSelector              = "#year"

    def labelSelector(index: Int): String = s"#statePensionLumpSumStartDate > div:nth-child($index) > div > label"
  }

  def statePensionLumpSumStartDateUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-date"

  private lazy val underTest = inject[StatePensionLumpSumStartDateView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    def form: Form[LocalDate] = formProvider(
      "statePensionLumpSumStartDate",
      altErrorPrefix = "pensions.statePensionLumpSumStartDate",
      earliestDateAndError = Some((EarliestDate, "pensions.statePensionLumpSumStartDate.error.localDate.tooLongAgo")),
      latestDateAndError = Some((PresentDate, "pensions.statePensionLumpSumStartDate.error.localDate.dateInFuture"))
    )

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(
              pensions = aPensionsCYAModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = None))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        val htmlFormat                  = underTest(form, taxYearEOY)
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
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData,
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val htmlFormat                  = underTest(form.fill(validDate), taxYearEOY)
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
