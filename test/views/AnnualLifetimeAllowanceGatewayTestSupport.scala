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

import controllers.pensions.lifetimeAllowance.routes.AnnualLifetimeAllowanceGatewayController
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.lifetimeAllowance.AnnualLifetimeAllowanceGatewayView

class AnnualLifetimeAllowanceGatewayTestSupport extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
    val findOutMoreSelector: String = s"#findOutMore-link"
    val calculatorLinkSelector: String = s"#taxCalculator-link"
    val line1Selector: String = s"#line1"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val expectedCalculatorText: String
    val expectedFindOutMoreText: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedCalculatorText = "if you need to work out your annual allowance (opens in new tab)"
    override val expectedFindOutMoreText = "Find out more about lifetime allowance (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedCalculatorText = "os oes angen i chi gyfrifo’ch lwfans blynyddol (opens in new tab)"
    override val expectedFindOutMoreText = "Dysgwch ragor am lwfans oes (opens in new tab)"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Have you gone above your annual allowance or lifetime allowance?"
    override val expectedErrorText = "Select yes if you have gone above your annual allowance or lifetime allowance"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedText = "Your pension providers would have told you if you went above your lifetime allowance."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Has your client gone above their annual allowance or lifetime allowance?"
    override val expectedErrorText = "Select yes if your client has gone above their annual allowance or lifetime allowance"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedText = "Your client’s pension providers would have told them if they went above their lifetime allowance."

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A ydych wedi mynd yn uwch na’ch lwfans blynyddol neu lwfans oes?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText = "Dewiswch ‘Iawn’ os ydych wedi mynd yn uwch na’ch lwfans blynyddol neu lwfans oes"
    override val expectedText = "Byddai’ch darparwyr pensiwn wedi rhoi gwybod i chi pe baech yn mynd dros eich lwfans oes."

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A yw’ch cleient wedi mynd dros ei lwfans blynyddol neu lwfans oes?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText = "Dewiswch ‘Iawn’ os yw’ch cleient wedi mynd dros ei lwfans blynyddol neu lwfans oes"
    override val expectedText = "Byddai darparwyr pensiwn eich cleient wedi rhoi gwybod iddo os oedd wedi mynd dros ei lwfans oes."
  }


  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[AnnualLifetimeAllowanceGatewayView]

  userScenarios.foreach { userScenario =>

    val form = YesNoForm.yesNoForm(
      missingInputError = s"AnnualAndLifetimeAllowance.gateway.error.${if (userScenario.isAgent) "agent" else "individual"}"
    )

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and no value selected" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedText, Selectors.line1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCalculatorText, Selectors.calculatorLinkSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(AnnualLifetimeAllowanceGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with filled in form using selected 'Yes' value" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.fill(value = true)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedText, Selectors.line1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCalculatorText, Selectors.calculatorLinkSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(AnnualLifetimeAllowanceGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with filled in form using selected 'No' value" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.fill(value = false)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedText, Selectors.line1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCalculatorText, Selectors.calculatorLinkSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        formPostLinkCheck(AnnualLifetimeAllowanceGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedText, Selectors.line1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCalculatorText, Selectors.calculatorLinkSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(AnnualLifetimeAllowanceGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}