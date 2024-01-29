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
import views.html.pensions.incomeFromPensions.TaxPaidOnStatePensionLumpSumView

class TaxPaidOnStatePensionLumpSumViewSpec extends ViewUnitTest with FakeRequestProvider {

  private val poundPrefixText = "£"
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
    val findOutLinkSelector            = "#annual-allowance-link"
    val overLimitLinkSelector          = "#over-limit-link"
    val detailsSelector                = "#main-content > div > div > form > details > summary > span"

    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedDetailsBullet1: String
    val expectedDetailsBullet2: String
    val emptyErrorText: String
    val maxAmountErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedDetailsTitle: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val amountHeading: String
    val amountHint: String
    val incorrectFormatErrorText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle          = "Did you pay tax on the State Pension lump sum?"
    val expectedHeading        = "Did you pay tax on the State Pension lump sum?"
    val expectedErrorTitle     = s"Error: $expectedTitle"
    val expectedError          = "Select yes if you paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "your P60"
    val expectedDetailsBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent you"
    val emptyErrorText         = "Enter the total amount of tax paid on the State Pension lump sum"
    val maxAmountErrorText     = "The amount of tax paid on the State Pension lump sum must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle          = "A wnaethoch chi dalu treth ar gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedHeading        = "A wnaethoch chi dalu treth ar gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedErrorTitle     = s"Gwall: $expectedTitle"
    val expectedError          = "Dewiswch ‘Iawn’ os gwnaethoch dalu treth ar gyfandaliad Pensiwn y Wladwriaeth"
    val expectedDetailsBullet1 = "eich P60"
    val expectedDetailsBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd atoch gan y Gwasanaeth Pensiwn"
    val emptyErrorText         = "Nodwch gyfanswm y dreth a dalwyd ar gyfandaliad Pensiwn y Wladwriaeth"
    val maxAmountErrorText     = "Mae’n rhaid i swm y dreth a dalwyd ar gyfandaliad Pensiwn y Wladwriaeth fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle          = "Did your client pay tax on the State Pension lump sum?"
    val expectedHeading        = "Did your client pay tax on the State Pension lump sum?"
    val expectedErrorTitle     = s"Error: $expectedTitle"
    val expectedError          = "Select yes if your client paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "your client’s P60"
    val expectedDetailsBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent your client"
    val emptyErrorText         = "Enter the amount of tax your client paid on the State Pension lump sum"
    val maxAmountErrorText     = "The amount of tax your client paid on the State Pension lump sum must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle          = "A wnaeth eich cleient dalu treth ar gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedHeading        = "A wnaeth eich cleient dalu treth ar gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedErrorTitle     = s"Gwall: $expectedTitle"
    val expectedError          = "Dewiswch ‘Iawn’ os gwnaeth eich cleient dalu treth ar gyfandaliad Pensiwn y Wladwriaeth"
    val expectedDetailsBullet1 = "P60 eich cleient"
    val expectedDetailsBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd at eich cleient gan y Gwasanaeth Pensiwn"
    val emptyErrorText         = "Nodwch swm y dreth a dalodd eich cleient ar y cyfandaliad Pensiwn y Wladwriaeth"
    val maxAmountErrorText = "Mae’n rhaid i swm y dreth a dalodd eich cleient ar y cyfandaliad Pensiwn y Wladwriaeth fod yn llai na £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedDetailsTitle           = "Where to find this information"
    val expectedButtonText             = "Continue"
    val yesText                        = "Yes"
    val noText                         = "No"
    val amountHeading                  = "Amount of tax paid"
    val amountHint                     = "For example, £193.54"
    val incorrectFormatErrorText       = "Enter the total amount of tax paid on the State Pension lump sum in pounds"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedDetailsTitle           = "Ble i ddod o hyd i’r wybodaeth hon"
    val expectedButtonText             = "Yn eich blaen"
    val yesText                        = "Iawn"
    val noText                         = "Na"
    val amountHeading                  = "Swm y dreth a dalwyd"
    val amountHint                     = "Er enghraifft, £193.54"
    val incorrectFormatErrorText       = "Nodwch gyfanswm y dreth a dalwyd ar gyfandaliad Pensiwn y Wladwriaeth yn y fformat cywir"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[TaxPaidOnStatePensionLumpSumView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the page with no data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, detailsSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.noText, 2, checked = false)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, continueButtonSelector)
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

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "142.24")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, detailsSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.noText, 2, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.amountHeading, Selectors.amountHeadingSelector)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, Selectors.hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, Selectors.amountValueSelector, "142.24")
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, continueButtonSelector)
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

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, "#value")
      }

      "render page with error when no lump sum tax paid amount is submitted" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

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

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100wrong")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.commonExpectedResults.incorrectFormatErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.commonExpectedResults.incorrectFormatErrorText)
      }

      "render page with over-maximum error when no lump sum tax paid amount has wrong amount" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(
            aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = anIncomeFromPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest
          )

        def form: Form[(Boolean, Option[BigDecimal])] =
          new FormsProvider().taxPaidOnStatePensionLumpSum(if (userScenario.isAgent) anAgentUser else aUser)

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
