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

package views.pensions.transferIntoOverseasPensions

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
import views.html.pensions.transferIntoOverseasPensions.pensionSchemeTaxTransferChargeView

class OverseasPensionTransferTaxChargeSpec extends ViewUnitTest with FakeRequestProvider{
  
  object Selectors{
    val captionSelector = "#main-content > div > div > header > p"
    val amountLabelSelector = "#conditional-value > div > label"
    val amountTextSelector = "#amount-2"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedAmountText: String
    val yes: String
    val no: String
    val continue: String
    val expectedTooBigErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedNoEntryErrorText: String
    val expectedNoAmountEntryErrorText: String
    val expectedIncorrectFormatErrorText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle = "Did your pension schemes pay tax on the amount that resulted in a transfer charge?"
    override val expectedNoEntryErrorText: String = "Select yes if your pension schemes paid tax on the amount on which you paid an overseas transfer charge"
    override val expectedIncorrectFormatErrorText: String = "Enter the tax paid on the amount on which you paid an overseas transfer charge in the correct format"
    override val expectedNoAmountEntryErrorText: String = "Enter the tax paid on the amount on which you paid an overseas transfer charge"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle = "A wnaeth eich cynlluniau pensiwn dalu treth ar y swm a wnaeth arwain at dâl trosglwyddiadau?"
    override val expectedNoEntryErrorText: String = "Dewiswch ‘Iawn’ os gwnaeth eich cynlluniau pensiwn dalu treth ar y swm y gwnaethoch chi dalu tâl trosglwyddiadau tramor arno"
    override val expectedIncorrectFormatErrorText: String = "Nodwch y dreth a dalwyd ar y swm y gwnaethoch dalu tâl trosglwyddiadau tramor arno yn y fformat cywir"
    override val expectedNoAmountEntryErrorText: String = "Nodwch y dreth a dalwyd ar y swm y gwnaethoch dalu tâl trosglwyddiadau tramor arno"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle = "Did your client’s pension schemes pay tax on the amount that resulted in a transfer charge?"
    override val expectedNoEntryErrorText: String = "Select yes if your client’s pension schemes paid tax on the amount that resulted in a transfer charge"
    override val expectedIncorrectFormatErrorText: String = "Enter the amount of tax paid on the transfer charge amount in the correct format"
    override val expectedNoAmountEntryErrorText: String = "Enter the amount of tax paid on the transfer charge amount"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle = "A wnaeth cynlluniau pensiwn eich cleient dalu treth ar y swm a wnaeth arwain at dâl trosglwyddiadau?"
    override val expectedNoEntryErrorText: String = "Dewiswch ‘Iawn’ os gwnaeth cynlluniau pensiwn eich cleient dalu treth ar y swm a wnaeth arwain at dâl trosglwyddiadau"
    override val expectedIncorrectFormatErrorText: String = "Nodwch swm y dreth a dalwyd ar y tâl trosglwyddiadau yn y fformat cywir"
    override val expectedNoAmountEntryErrorText: String = "Nodwch swm y dreth a dalwyd ar y tâl trosglwyddiadau"
  }
  
  object ExpectedCommonEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedAmountText: String = "Total tax, in pounds"
    override val yes: String = "Yes"
    override val no: String = "No"
    override val continue: String = "Continue"
    override val expectedTooBigErrorText: String = "The amount of tax paid on the transfer charge amount must be less than £100,000,000,000"
  }

  object ExpectedCommonCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Trosglwyddiadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedAmountText: String = "Cyfanswm y dreth, mewn punnoedd"
    override val yes: String = "Iawn"
    override val no: String = "Na"
    override val continue: String = "Yn eich blaen"
    override val expectedTooBigErrorText: String = "Mae’n rhaid i swm y dreth a dalwyd ar y tâl trosglwyddiadau fod yn llai na £100,000,000,000"
  }
  
  
  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedCommonEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, ExpectedCommonEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, ExpectedCommonCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, ExpectedCommonCY, Some(ExpectedAgentCY))
  )



  private lazy val underTest = inject[pensionSchemeTaxTransferChargeView]
  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page without pre filled date" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continue)
      }
      "render the page with pre filled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100.00")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)


        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        inputFieldValueCheck("amount-2", Selectors.amountTextSelector, "100")
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continue)
      }

      "render the page with an error when the user doesn’t select a radio button" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, Some("value"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, "#value")
      }

      "render the page with an error when the user selects yes but doesn’t enter the amount" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoAmountEntryErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoAmountEntryErrorText, "#amount-2")
      }

      "render the page with an error when the user selects yes but amount is in wrong format" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "bfsbrfg")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorText, "#amount-2")
      }
      "render the page with an error when the user selects yes but amount exceeds max" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().pensionSchemeTaxTransferForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "1000000000000000000000.00")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.commonExpectedResults.expectedTooBigErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.expectedTooBigErrorText, "#amount-2")
      }
    }}



}
