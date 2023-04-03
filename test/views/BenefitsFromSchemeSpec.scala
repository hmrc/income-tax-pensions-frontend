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

import controllers.pensions.employmentFinancedRetirementSchemes.routes.BenefitsFromSchemeController
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.employmentFinancedRetirementSchemes.BenefitsFromSchemeView

class BenefitsFromSchemeSpec extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
    val paragraphSelector: Int => String = (index: Int) => s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitleText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val paragraph1: String
    val paragraph2: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedErrorText: String
    val expectedQuestionText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) =>
      s"Employer-financed retirement benefits schemes for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitleText: String = "Employer-financed retirement benefits schemes"
    val expectedYesText: String = "Yes"
    val expectedNoText: String = "No"
    val expectedButtonText: String = "Continue"
    val paragraph1: String =
      "If a pension scheme is not a registered it might be an employer-financed retirement benefit scheme (EFRBS)."
    val paragraph2: String =
      "An EFRBS is a scheme that pays certain retirement or death benefits for employees or former employees called 'relevant benefits'."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) =>
      s"Cynlluniau buddiannau ymddeol a ariannwyd gan gyflogwyr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitleText: String = "Cynlluniau buddiannau ymddeol a ariannwyd gan gyflogwyr"
    val expectedYesText: String = "Iawn"
    val expectedNoText: String = "Na"
    val expectedButtonText: String = "Yn eich blaen"
    val paragraph1: String =
      "Os nad yw cynllun pensiwn yn gofrestredig, gallai fod yn gynllun buddiant ymddeol a ariannwyd gan gyflogwyr (EFRBS)."
    val paragraph2: String =
      "Cynllun yw EFRBS sy’n talu rhai buddiannau ymddeoliad neu farwolaeth ar gyfer gweithwyr neu gyn-weithwyr, a elwir yn ‘fuddiannau perthnasol’."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitleText: String = "Employer-financed retirement benefits schemes"
    val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    val expectedErrorText: String = "Select yes if you have an employer-financed retirement benefits scheme"
    val expectedQuestionText: String = "Do you have an employer-financed retirement scheme?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitleText: String = "Employer-financed retirement benefits schemes"
    val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    val expectedErrorText: String = "Select yes if your client has an employer-financed retirement benefits scheme"
    val expectedQuestionText: String = "Does your client have an employer-financed retirement scheme?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitleText: String = "Cynlluniau buddiannau ymddeol a ariannwyd gan gyflogwyr"
    val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    val expectedErrorText: String = "Dewiswch ‘Iawn’ os oes gennych gynllun buddiannau ymddeol a ariannwyd gan gyflogwyr"
    val expectedQuestionText: String = "A oes gennych gynllun ymddeol a ariannwyd gan gyflogwr?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitleText: String = "Cynlluniau buddiannau ymddeol a ariannwyd gan gyflogwyr"
    val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    val expectedErrorText: String =
      "Dewiswch ‘Iawn’ os oes gan eich cleient gynllun buddiannau ymddeol a ariannwyd gan gyflogwyr"
    val expectedQuestionText: String = "A oes gan eich cleient gynllun ymddeol a ariannwyd gan gyflogwr?"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[BenefitsFromSchemeView]

  userScenarios.foreach { userScenario =>

    val form = YesNoForm.yesNoForm(
      missingInputError =
        s"employerFinancedRetirementScheme.benefitsFromScheme.error.${if (userScenario.isAgent) "agent" else "individual"}"
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
        textOnPageCheck(userScenario.commonExpectedResults.paragraph1, Selectors.paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.paragraph2, Selectors.paragraphSelector(2))
        h1Check(userScenario.specificExpectedResults.get.expectedQuestionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(BenefitsFromSchemeController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.paragraph1, Selectors.paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.paragraph2, Selectors.paragraphSelector(2))
        h1Check(userScenario.specificExpectedResults.get.expectedQuestionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(BenefitsFromSchemeController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.paragraph1, Selectors.paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.paragraph2, Selectors.paragraphSelector(2))
        h1Check(userScenario.specificExpectedResults.get.expectedQuestionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        formPostLinkCheck(BenefitsFromSchemeController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.commonExpectedResults.paragraph1, Selectors.paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.paragraph2, Selectors.paragraphSelector(2))
        h1Check(userScenario.specificExpectedResults.get.expectedQuestionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(BenefitsFromSchemeController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
