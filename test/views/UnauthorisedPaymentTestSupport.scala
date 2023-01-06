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

package views

import controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentController
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentView

class UnauthorisedPaymentTestSupport extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
    val findOutMoreSelector: String = s"#findOutMore-link"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitleText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val expectedFindOutMoreText: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "Did you get any unauthorised payment from a pension scheme?"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedFindOutMoreText = "Find out more about unauthorised payments (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedTitleText: String = "A gawsoch unrhyw daliad heb awdurdod o gynllun pensiwn?"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedFindOutMoreText = "Dysgwch ragor am daliadau heb awdurdod (opens in new tab)"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did you get any unauthorised payment from a pension scheme?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if you got unauthorised payments"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did your client get any unauthorised payment from a pension scheme?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if your client got unauthorised payments"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A gawsoch unrhyw daliad heb awdurdod o gynllun pensiwn?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os cawsoch daliadau heb awdurdod"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A gafodd eich cleient unrhyw daliad heb awdurdod o gynllun pensiwn?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os cafodd eich cleient daliadau heb awdurdod"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[UnauthorisedPaymentView]

  userScenarios.foreach { userScenario =>

    val form = YesNoForm.yesNoForm(
      missingInputError = s"unauthorisedPayments.gateway.error.${if (userScenario.isAgent) "agent" else "individual"}"
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
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(UnauthorisedPaymentController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(UnauthorisedPaymentController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        formPostLinkCheck(UnauthorisedPaymentController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.commonExpectedResults.expectedFindOutMoreText, Selectors.findOutMoreSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(UnauthorisedPaymentController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
