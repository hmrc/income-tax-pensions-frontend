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

package views.shortServiceRefunds

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonAmountForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.twirl.api.HtmlFormat
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.shortServiceRefunds.TaxableRefundAmountView

class ShortServiceTaxableRefundAmountSpec extends ViewUnitTest with FakeRequestProvider{
  
  object Selectors{
    val captionSelector = "#main-content > div > div > header > p"
    val amountLabelSelector = "#conditional-value > div > label"
    val amountTextSelector = "#amount-2"
    val p1Selector = "#main-content > div > div > p:nth-child(2)"
    val p2Selector = "#main-content > div > div > p:nth-child(3)"
    val detailsTitle = "#main-content > div > div > details > summary"
    val detailsP1Selector = "#main-content > div > div > details > div > p:nth-child(1)"
    val detailsP2Selector = "#main-content > div > div > details > div > p:nth-child(2)"
    val h2Selector = "#main-content > div > div > h2"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedCaption: Int => String
    val expectedAmountText: String
    val expectedAmountHint: String
    val yes: String
    val no: String
    val continue: String
    val expectedDetailsTitle: String
  }

  trait SpecificExpectedResults {
    val expectedP1: String
    val expectedP2: String
    val h2: String
    val expectedDetailsP1: String
    val expectedDetailsP2: String
    val expectedNoEntryErrorText: String
    val expectedNoAmountEntryErrorText: String
    val expectedIncorrectFormatErrorText: String
    val expectedTooBigErrorText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedP1: String = "The short service refund is taxable if you got UK tax relief on the money you paid into the scheme."
    override val expectedP2: String = "Tell us the total amount of short service refund you got from overseas pension schemes."
    override val expectedDetailsP1: String = "A short service refund is a refund of money you paid into a workplace pension."
    override val expectedDetailsP2: String = "You might have got a short service refund if you paid into a scheme for less than 2 years. This depends on the type of pension scheme you have."
    override val h2: String = "Did you get a short service refund?"
    override val expectedNoEntryErrorText: String = "Select yes if you got a taxable short service refund from an overseas pension scheme"
    override val expectedNoAmountEntryErrorText: String = "Enter the taxable short service refund amount"
    override val expectedIncorrectFormatErrorText: String = "Enter the taxable short service refund amount in the correct format"
    override val expectedTooBigErrorText: String = "The taxable short service refund amount must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedP1: String = "The short service refund is taxable if you got UK tax relief on the money you paid into the scheme."
    override val expectedP2: String = "Tell us the total amount of short service refund you got from overseas pension schemes."
    override val expectedDetailsP1: String = "A short service refund is a refund of money you paid into a workplace pension."
    override val expectedDetailsP2: String = "You might have got a short service refund if you paid into a scheme for less than 2 years. This depends on the type of pension scheme you have."
    override val h2: String = "Did you get a short service refund?"
    override val expectedNoEntryErrorText: String = "Select yes if you got a taxable short service refund from an overseas pension scheme"
    override val expectedNoAmountEntryErrorText: String = "Enter the taxable short service refund amount"
    override val expectedIncorrectFormatErrorText: String = "Enter the taxable short service refund amount in the correct format"
    override val expectedTooBigErrorText: String = "The taxable short service refund amount must be less than £100,000,000,000"
  }

    object ExpectedAgentEN extends SpecificExpectedResults {
      override val expectedP1: String = "The short service refund is taxable if your client got UK tax relief on the money they paid into the scheme."
      override val expectedP2: String = "Tell us the total amount of short service refund your client got from overseas pension schemes."
      override val expectedDetailsP1: String = "A short service refund is a refund of money your client paid into a workplace pension."
      override val expectedDetailsP2: String = "Your client might have got a short service refund if they paid into a scheme for less than 2 years. This depends on the type of pension scheme they have."
      override val h2: String = "Did your client get a short service refund?"
      override val expectedNoEntryErrorText: String = "Select yes if your client got a taxable short service refund from an overseas pension scheme"
      override val expectedNoAmountEntryErrorText: String = "Enter your client’s taxable short service refund amount"
      override val expectedIncorrectFormatErrorText: String = "Enter your client’s taxable short service refund amount in the correct format"
      override val expectedTooBigErrorText: String = "Your client’s taxable short service refund amount must be less than £100,000,000,000"
    }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedP1: String = "The short service refund is taxable if your client got UK tax relief on the money they paid into the scheme."
    override val expectedP2: String = "Tell us the total amount of short service refund your client got from overseas pension schemes."
    override val expectedDetailsP1: String = "A short service refund is a refund of money your client paid into a workplace pension."
    override val expectedDetailsP2: String = "Your client might have got a short service refund if they paid into a scheme for less than 2 years. This depends on the type of pension scheme they have."
    override val h2: String = "Did your client get a short service refund?"
    override val expectedNoEntryErrorText: String = "Select yes if your client got a taxable short service refund from an overseas pension scheme"
    override val expectedNoAmountEntryErrorText: String = "Enter your client’s taxable short service refund amount"
    override val expectedIncorrectFormatErrorText: String = "Enter your client’s taxable short service refund amount in the correct format"
    override val expectedTooBigErrorText: String = "Your client’s taxable short service refund amount must be less than £100,000,000,000"
  }
  
  object ExpectedCommonEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Taxable short service refunds"
    override val expectedAmountText: String = "Total amount of short service refunds, in pounds"
    override val expectedAmountHint: String = "For example, £193.54"
    override val yes: String = "Yes"
    override val no: String = "No"
    override val continue: String = "Continue"
    override val expectedDetailsTitle: String = "What is a short service refund?"
  }

  object ExpectedCommonCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Taxable short service refunds"
    override val expectedAmountText: String = "Total amount of short service refunds, in pounds"
    override val expectedAmountHint: String = "For example, £193.54"
    override val yes: String = "Yes"
    override val no: String = "No"
    override val continue: String = "Continue"
    override val expectedDetailsTitle: String = "What is a short service refund?"
  }
  
  
  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedCommonEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, ExpectedCommonEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, ExpectedCommonCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, ExpectedCommonCY, Some(ExpectedAgentCY))
  )


  private lazy val underTest = inject[TaxableRefundAmountView]
  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page without pre filled date" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.p1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2, Selectors.p2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.detailsTitle)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsP1, Selectors.detailsP1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsP2, Selectors.detailsP2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.h2, Selectors.h2Selector)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continue)
      }
      "render the page with pre filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100.00")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.p1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2, Selectors.p2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedDetailsTitle, Selectors.detailsTitle)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsP1, Selectors.detailsP1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsP2, Selectors.detailsP2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.h2, Selectors.h2Selector)
        inputFieldValueCheck("amount-2", Selectors.amountTextSelector, "100")
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continue)
      }

      "render the page with an error when the user doesn’t select a radio button" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, Some("value"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, "#value")
      }

      "render the page with an error when the user selects yes but doesn’t enter the amount" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoAmountEntryErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoAmountEntryErrorText, "#amount-2")
      }

      "render the page with an error when the user selects yes but amount is in wrong format" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "bfsbrfg")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorText, "#amount-2")
      }
      "render the page with an error when the user selects yes but amount exceeds max" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().shortServiceTaxableRefundForm(if(userScenario.isAgent) anAgentUser else aUser)
        implicit val htmlFormat: HtmlFormat.Appendable = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "1000000000000000000000.00")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedTooBigErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedTooBigErrorText, "#amount-2")
      }
    }}



}
