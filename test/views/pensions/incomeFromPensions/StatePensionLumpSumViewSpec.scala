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
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonAmountForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.incomeFromPensions.StatePensionLumpSumView

class StatePensionLumpSumViewSpec extends ViewUnitTest with FakeRequestProvider {

  private val amountInputName = "amount-2"

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val amountHeadingSelector          = "#conditional-value > div > label"
    val amountValueSelector            = "#amount-2"
    val expectedErrorHref              = "#amount-2"
    val hintTextSelector: String       = "#amount-2-hint"
    val poundPrefixSelector            = ".govuk-input__prefix"
    val whereToFindSelector            = "#main-content > div > div > form > details > summary > span"
    val detailsSelector                = "#main-content > div > div > form > details > summary > span"

    def paragraphSelector(index: Int, withError: Boolean = false): String =
      s"#main-content > div > div > p:nth-child(${index + (if (withError) 2 else 1)})"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedErrorTitle: String
    val expectedError: String
    val emptyErrorText: String
    val incorrectFormatErrorText: String
    val maxAmountErrorText: String
    val expectedP1: String
    val expectedBullet1: String
    val expectedBullet2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedWhereToFind: String
    val expectedP2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle      = "Did you get a State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError      = "Select yes if you got a State Pension lump sum"
    val expectedP1         = "You might have received a one-off lump sum payment if you delayed claiming your State Pension for 12 months in a row."
    val emptyErrorText     = "Enter the amount of your State Pension lump sum"
    val incorrectFormatErrorText = "Enter your State Pension lump sum amount in pounds"
    val maxAmountErrorText       = "Your State Pension lump sum amount must be less than £100,000,000,000"
    val expectedBullet1          = "your P60"
    val expectedBullet2          = "the ‘About general increases in benefits‘ letter, the Pension Service sent you"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle      = "A gawsoch gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError      = "Dewiswch ‘Iawn’ os cawsoch gyfandaliad Pensiwn y Wladwriaeth"
    val expectedP1 =
      "Mae’n bosibl y byddwch wedi cael cyfandaliad unigol os oeddech wedi oedi cyn hawlio’ch Pensiwn y Wladwriaeth am 12 mis yn olynol."
    val emptyErrorText           = "Nodwch swm eich cyfandaliad Pensiwn y Wladwriaeth"
    val incorrectFormatErrorText = "Nodwch swm eich cyfandaliad Pensiwn y Wladwriaeth yn y fformat cywir"
    val maxAmountErrorText       = "Mae’n rhaid i swm eich cyfandaliad Pensiwn y Wladwriaeth fod yn llai na £100,000,000,000"
    val expectedBullet1          = "eich P60"
    val expectedBullet2          = "y llythyr ‘Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd atoch gan y Gwasanaeth Pensiwn"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle      = "Did your client get a State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError      = "Select yes if your client got a State Pension lump sum"
    val expectedP1 = "Your client might have received a one-off lump sum payment if they delayed claiming their State Pension for 12 months in a row."
    val emptyErrorText           = "Enter the amount of your client’s State Pension lump sum"
    val incorrectFormatErrorText = "Enter your client’s State Pension lump sum amount in pounds"
    val maxAmountErrorText       = "Your client’s State Pension lump sum amount must be less than £100,000,000,000"
    val expectedBullet1          = "your client’s P60"
    val expectedBullet2          = "the ‘About general increases in benefits‘ letter, the Pension Service sent your client"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle      = "A gafodd eich cleient gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError      = "Dewiswch ‘Iawn’ os cafodd eich cleient gyfandaliad Pensiwn y Wladwriaeth"
    val expectedP1 =
      "Mae’n bosibl y byddai’ch cleient wedi cael cyfandaliad unigol os oedden nhw wedi oedi cyn hawlio ei Bensiwn y Wladwriaeth am 12 mis yn olynol."
    val emptyErrorText           = "Nodwch swm cyfandaliad Pensiwn y Wladwriaeth eich cleient"
    val incorrectFormatErrorText = "Nodwch swm cyfandaliad Pensiwn y Wladwriaeth eich cleient yn y fformat cywir"
    val maxAmountErrorText       = "Mae’n rhaid i swm cyfandaliad Pensiwn y Wladwriaeth eich cleient fod yn llai na £100,000,000,000"
    val expectedBullet1          = "P60 eich cleient"
    val expectedBullet2          = "y llythyr ‘Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd at eich cleient gan y Gwasanaeth Pensiwn"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedWhereToFind            = "Where to find this information"
    val expectedP2                     = "This only applies to people who reach State Pension age before 6 April 2016."
    val expectedButtonText             = "Continue"
    val yesText                        = "Yes"
    val noText                         = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedWhereToFind            = "Ble i ddod o hyd i’r wybodaeth hon"
    val expectedP2                     = "Mae hyn ond yn berthnasol i bobl sy’n cyrraedd oedran Pensiwn y Wladwriaeth cyn 6 Ebrill 2016."
    val expectedButtonText             = "Yn eich blaen"
    val yesText                        = "Iawn"
    val noText                         = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[StatePensionLumpSumView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'Did you get a State Pension lump sum?' page with correct content and no pre-filling" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedP2, paragraphSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedWhereToFind, whereToFindSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "142.24")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedP2, paragraphSelector(2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedWhereToFind, whereToFindSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        inputFieldValueCheck(amountInputName, Selectors.amountValueSelector, "142.24")
        buttonCheck(expectedButtonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when no data is submitted" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, "#value")
      }

      "render page with error when no State Pension lump sum amount is submitted" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.emptyErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyErrorText)
      }

      "render page with incorrect-format error when no lump sum tax paid amount has wrong amount" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100wrong")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.incorrectFormatErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.incorrectFormatErrorText)
      }

      "render page with over-maximum error when no lump sum tax paid amount has wrong amount" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().statePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat =
          underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "£100,000,000,042")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.maxAmountErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.maxAmountErrorText)
      }
    }
  }
}
