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

package views.paymentsIntoOverSeasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonForm}
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.PensionReliefTypeView

class PensionsReliefViewSpec extends ViewUnitTest with FakeRequestProvider {

  object Selectors {
    val captionSelector = "#main-content > div > div > header > p"
    val titleSelector = "#main-content > div > div > header > h1"
    val buttonSelector = "#continue"
    val orSelector = "#main-content > div > div > form > div > fieldset > div > div.govuk-radios__divider"
    val linkSelector = "#find-out-more-link"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedMMR: String
    val expectedDTR: String
    val expectedTCR: String
    val noneOfTheAbove: String
    val or: String
    val expectedLinkText: String
    val expectedErrorText: String
    val continue: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String

  }

  object ExpectedCommonEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedMMR: String = "Migrant member relief"
    override val expectedDTR: String = "Double taxation relief"
    override val expectedTCR: String = "Transitional corresponding relief"
    override val noneOfTheAbove: String = "None of these"
    override val or: String = "or"
    override val expectedLinkText: String = "Find out about the types of tax relief for overseas pension scheme payments (opens in new tab)"
    override val expectedErrorText: String = "Select the type of tax relief or select none"
    override val continue: String = "Continue"

  }

  object ExpectedCommonCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedMMR: String = "Migrant member relief"
    override val expectedDTR: String = "Double taxation relief"
    override val expectedTCR: String = "Transitional corresponding relief"
    override val noneOfTheAbove: String = "None of these"
    override val or: String = "neu"
    override val expectedLinkText: String = "Find out about the types of tax relief for overseas pension scheme payments (opens in new tab)"
    override val expectedErrorText: String = "Select the type of tax relief or select none"
    override val continue: String = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "What tax relief did you get on payments into overseas pensions?"
    override val expectedErrorTitle = s"Error: $expectedTitle"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "What tax relief did you get on payments into overseas pensions?"
    override val expectedErrorTitle = s"Error: $expectedTitle"

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "What tax relief did your client get on payments into overseas pensions?"
    override val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "What tax relief did your client get on payments into overseas pensions?"
    override val expectedErrorTitle = s"Error: $expectedTitle"
  }



  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedCommonEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, ExpectedCommonEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, ExpectedCommonCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, ExpectedCommonCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[PensionReliefTypeView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.
              copy(reliefs = Seq.empty[Relief]))),
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[String] = new FormsProvider().overseasPensionsReliefTypeForm
        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(1)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        buttonCheck(userScenario.commonExpectedResults.continue)
        radioButtonCheck(userScenario.commonExpectedResults.expectedMMR, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedDTR, 2, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedTCR, 3, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.or, Selectors.orSelector)
        radioButtonCheck(userScenario.commonExpectedResults.noneOfTheAbove, 4, checked = false)

        linkCheck(userScenario.commonExpectedResults.expectedLinkText, Selectors.linkSelector,
          href = "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions",
          isExactUrlMatch = false)

      }
      "render page with pre filled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[String] = new FormsProvider().overseasPensionsReliefTypeForm

        implicit val document: Document = Jsoup.parse(underTest(form.fill(
            aPensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.head.reliefType.get),
            taxYearEOY,
            Some(0)).body
        )

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        buttonCheck(userScenario.commonExpectedResults.continue)
        radioButtonCheck(userScenario.commonExpectedResults.expectedMMR, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedDTR, 2, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedTCR, 3, checked = true)
        textOnPageCheck(userScenario.commonExpectedResults.or, Selectors.orSelector)
        radioButtonCheck(userScenario.commonExpectedResults.noneOfTheAbove, 4, checked = false)

        linkCheck(userScenario.commonExpectedResults.expectedLinkText, Selectors.linkSelector,
          href = "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions",
          isExactUrlMatch = false)
      }
      "render page with error text when no option was selected" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[String] = new FormsProvider().overseasPensionsReliefTypeForm

        val htmlFormat = underTest(form.bind(Map(RadioButtonForm.value -> "")), taxYearEOY, Some(1))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.expectedErrorText, Some("value"))
        errorSummaryCheck(userScenario.commonExpectedResults.expectedErrorText, "#value")
      }
    }
  }
}
