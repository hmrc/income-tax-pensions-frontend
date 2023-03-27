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

import forms.{PaymentsIntoOverseasPensionsFormProvider, YesNoForm}
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.PaymentsIntoOverseasPensionsView

class PaymentsIntoOverseasPensionsGatewaySpec extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
    val paragraphSelector: String = "#main-content > div > div > p:nth-of-type(1)"
    val listItemFirstSelector: String = "#main-content > div > div > ul:nth-of-type(1) > li:nth-of-type(1)"
    val listItemLastSelector: String = "#main-content > div > div > ul:nth-of-type(1) > li:nth-of-type(3)"
    val orSelector: String = "#main-content > div > div > ul:nth-of-type(1) > li:nth-of-type(2)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitleText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val expectedOrText: String
    val listItemFirstText: String
    val listItemLastText: String
  }

  trait SpecificExpectedResults {
    val expectedErrorTitleText: String
    val paragraphText: String
    val questionText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "Payments into overseas pensions"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedOrText: String = "or"
    override val listItemFirstText: String = "paid into an overseas pension scheme"
    override val listItemLastText: String = "transferred UK pension savings to an overseas pension scheme"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "Taliadau i bensiynau tramor"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedOrText: String = "neu"
    override val listItemFirstText: String = "talu i mewn i gynllun pensiwn tramor"
    override val listItemLastText: String = "trosglwyddo cynilion pensiwn y DU i gynllun pensiwn tramor"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedErrorText: String = "Select yes if you or your employer paid into an overseas pension scheme"
    override val paragraphText: String = "Tell us if you or your employer:"
    override val questionText: String = "Have you or your employer paid into overseas pensions?"
    override val expectedErrorTitleText: String = s"Error: Payments into overseas pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os gwnaethoch chi neu’ch cyflogwr dalu i mewn i gynllun pensiwn tramor"
    override val paragraphText: String = "Rhowch wybod i ni os ydych chi neu’ch cyflogwr wedi gwneud y canlynol:"
    override val questionText: String = "A ydych chi neu’ch cyflogwr wedi talu i mewn i bensiynau tramor?"
    override val expectedErrorTitleText: String = s"Error: Taliadau i bensiynau tramor"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedErrorText: String = "Select yes if your client or their employer paid into an overseas pension scheme"
    override val paragraphText: String = "Tell us if your client or their employer:"
    override val questionText: String = "Has your client or their employer paid into overseas pensions?"
    override val expectedErrorTitleText: String = s"Error: Payments into overseas pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os talodd eich cleient neu ei gyflogwr i mewn i gynllun pensiwn tramor"
    override val paragraphText: String = "Rhowch wybod i ni os yw’ch cleient neu ei gyflogwr wedi gwneud y canlynol:"
    override val questionText: String = "A yw’ch cleient neu ei gyflogwr wedi talu i mewn i bensiynau tramor?"
    override val expectedErrorTitleText: String = s"Error: Taliadau i bensiynau tramor"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[PaymentsIntoOverseasPensionsView]

  userScenarios.foreach { userScenario =>

    val form = new PaymentsIntoOverseasPensionsFormProvider().paymentsIntoOverseasPensionsForm(userScenario.isAgent)

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and no value selected" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraphText, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemFirstText, Selectors.listItemFirstSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedOrText, Selectors.orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemLastText, Selectors.listItemLastSelector)
        h1Check(userScenario.specificExpectedResults.get.questionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with filled in form using selected 'Yes' value" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.fill(value = true)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraphText, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemFirstText, Selectors.listItemFirstSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedOrText, Selectors.orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemLastText, Selectors.listItemLastSelector)
        h1Check(userScenario.specificExpectedResults.get.questionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }
//
      "render page with filled in form using selected 'No' value" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.fill(value = false)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraphText, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemFirstText, Selectors.listItemFirstSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedOrText, Selectors.orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemLastText, Selectors.listItemLastSelector)
        h1Check(userScenario.specificExpectedResults.get.questionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }
//
      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraphText, Selectors.paragraphSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemFirstText, Selectors.listItemFirstSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedOrText, Selectors.orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.listItemLastText, Selectors.listItemLastSelector)
        h1Check(userScenario.specificExpectedResults.get.questionText, size = "m")
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
