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

import controllers.pensions.paymentsIntoPension.PaymentsIntoPensionFormProvider
import controllers.pensions.paymentsIntoPension.routes.PaymentsIntoPensionsStatusController
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsStatusView

class PaymentsIntoPensionsStatusTestSupport extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitleText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "Did you make any payments into UK pensions?"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "A wnaethoch unrhyw daliadau i bensiynau’r DU?"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did you make any payments into UK pensions?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if you made payments into UK pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did your client make any payments into UK pensions?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if your client made payments into UK pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A wnaethoch unrhyw daliadau i bensiynau’r DU?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os gwnaethoch daliadau i bensiynau’r DU"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A wnaeth eich cleient unrhyw daliadau i bensiynau’r DU?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os gwnaeth eich cleient daliadau i bensiynau’r DU"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[PaymentsIntoPensionsStatusView]

  userScenarios.foreach { userScenario =>

    val form = new PaymentsIntoPensionFormProvider().paymentsIntoPensionsStatusForm(userScenario.isAgent)

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and no value selected" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(PaymentsIntoPensionsStatusController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(PaymentsIntoPensionsStatusController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        formPostLinkCheck(PaymentsIntoPensionsStatusController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(PaymentsIntoPensionsStatusController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
