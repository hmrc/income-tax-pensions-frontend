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

import controllers.pensions.incomeFromPensions.routes.IncomeFromPensionsGatewayController
import forms.{IncomeFromPensionFormProvider, YesNoForm}
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.incomeFromPensions.IncomeFromPensionsGatewayView

class IncomeFromPensionsGatewayTestSupport extends ViewUnitTest {

  object Selectors {
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
    val expectedParagraphTitleSelector: String = "#main-content > div > div > p"

    def expectedParagraphSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    val expectedDetailsTitleSelector: String = "#main-content > div > div > form > details > summary > span"
    val expectedDetailsSelector: String = "#main-content > div > div > form > details > div > p"

    def expectedDetailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val expectedParagraphTitle: String
    val expectedParagraph1: String
    val expectedParagraph2: String
    val expectedDetailsTitle: String
    val expectedDetails: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedErrorText: String
    val expectedDetails1: String
    val expectedDetails2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
    override val expectedParagraphTitle: String = "This includes:"
    override val expectedParagraph1: String = "State Pension"
    override val expectedParagraph2: String = "workplace and private pensions"
    override val expectedDetailsTitle: String = "Where to find this information"
    override val expectedDetails: String = "You can find this information in:"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedYesText: String = "Iawn"
    override val expectedNoText: String = "Na"
    override val expectedButtonText: String = "Yn eich blaen"
    override val expectedParagraphTitle: String = "Mae’r rhain yn cynnwys:"
    override val expectedParagraph1: String = "Pensiwn y Wladwriaeth"
    override val expectedParagraph2: String = "pensiynau gweithle a phreifat"
    override val expectedDetailsTitle: String = "Ble i ddod o hyd i’r wybodaeth hon"
    override val expectedDetails: String = "Gallwch ddod o hyd i’r wybodaeth hon yn:"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did you get income from pension schemes?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if you got payments from pension schemes"
    override val expectedDetails1: String = "your P60"
    override val expectedDetails2: String = "the ’About general increases in benefits’ letter the Pension Service sent you"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Did your client get income from pension schemes?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedErrorText: String = "Select yes if your client got payments from pension schemes"
    override val expectedDetails1: String = "your client’s P60"
    override val expectedDetails2: String = "the ’About general increases in benefits’ letter the Pension Service sent your client"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A gawsoch incwm o gynlluniau pensiwn?"
    override val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ’Iawn’ os cawsoch daliadau o gynlluniau pensiwn"
    override val expectedDetails1: String = "eich P60"
    override val expectedDetails2: String = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd atoch gan y Gwasanaeth Pensiwn"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A gafodd eich cleient incwm o gynlluniau pensiwn?"
    override val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    override val expectedErrorText: String = "Dewiswch ’Iawn’ os cafodd eich cleient daliadau o gynlluniau pensiwn"
    override val expectedDetails1: String = "P60 eich cleient"
    override val expectedDetails2: String = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd at eich cleient gan y Gwasanaeth Pensiwn"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[IncomeFromPensionsGatewayView]

  userScenarios.foreach { userScenario =>

    val form = new IncomeFromPensionFormProvider().incomeFromPensionsGatewayForm(userScenario.isAgent)


    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and no value selected" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraphTitle, Selectors.expectedParagraphTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph1, Selectors.expectedParagraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph2, Selectors.expectedParagraphSelector(2))
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.expectedDetailsTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetails, Selectors.expectedDetailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, Selectors.expectedDetailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails2, Selectors.expectedDetailsBulletSelector(2))
        formPostLinkCheck(IncomeFromPensionsGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraphTitle, Selectors.expectedParagraphTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph1, Selectors.expectedParagraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph2, Selectors.expectedParagraphSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.expectedDetailsTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetails, Selectors.expectedDetailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, Selectors.expectedDetailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails2, Selectors.expectedDetailsBulletSelector(2))
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(IncomeFromPensionsGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
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
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraphTitle, Selectors.expectedParagraphTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph1, Selectors.expectedParagraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph2, Selectors.expectedParagraphSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.expectedDetailsTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetails, Selectors.expectedDetailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, Selectors.expectedDetailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails2, Selectors.expectedDetailsBulletSelector(2))
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = true)
        formPostLinkCheck(IncomeFromPensionsGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, form = form.bind(Map(YesNoForm.yesNo -> ""))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraphTitle, Selectors.expectedParagraphTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph1, Selectors.expectedParagraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedParagraph2, Selectors.expectedParagraphSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.expectedDetailsTitleSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetails, Selectors.expectedDetailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, Selectors.expectedDetailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails2, Selectors.expectedDetailsBulletSelector(2))
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(IncomeFromPensionsGatewayController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
