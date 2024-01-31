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

package views.pensions.shortServiceRefunds

import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, RadioButtonAmountForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.shortServiceRefunds.NonUkTaxRefundsView

class NonUkTaxRefundsViewSpec extends ViewUnitTest {

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val amountLabelSelector            = "#conditional-value > div > label"
    val amountTextSelector             = "#amount-2"
    val p1Selector                     = "#main-content > div > div > p:nth-child(2)"
    val p2Selector                     = "#main-content > div > div > p:nth-child(3)"
    val questionSelector               = "#main-content > div > div > h2"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val continueButtonSelector: String = "#continue"
  }

  trait ExpectedContents {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedCaption: Int => String
    val expectedPara1: String
    val expectedPara2: String
    val expectedQuestion: String
    val expectedAmountText: String
    val expectedAmountHint: String
    val yes: String
    val no: String
    val continue: String
    val errorMessage: String
    val expectedNoAmountEntryErrorText: String
    val expectedIncorrectFormatErrorText: String
    val expectedTooBigErrorText: String
  }

  object ExpectedContentsIndividualEN extends ExpectedContents {
    val expectedTitle: String                    = "Non-UK tax on short service refunds"
    val expectedErrorTitle                       = s"Error: $expectedTitle"
    val expectedCaption: Int => String           = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedPara1: String                    = "If you paid non-UK tax on more than one refund, add the amounts together."
    val expectedPara2: String                    = "If you do not know this information, you can check with the employer or pension scheme provider."
    val expectedQuestion: String                 = "Did you pay non-UK tax on short service refunds?"
    val expectedAmountText: String               = "Total non-UK tax on short service refunds, in pounds"
    val expectedAmountHint: String               = "For example, £193.54"
    val yes: String                              = "Yes"
    val no: String                               = "No"
    val continue: String                         = "Continue"
    val errorMessage: String                     = "Select yes if you paid non-UK tax on this short service refund"
    val expectedNoAmountEntryErrorText: String   = "Enter the amount of non-UK tax you paid on this short service refund"
    val expectedIncorrectFormatErrorText: String = "Enter the amount of non-UK tax you paid on this short service refund in pounds"
    val expectedTooBigErrorText: String          = "The amount of non-UK tax you paid on this short service refund must be less than £100,000,000,000"
  }

  object ExpectedContentsAgentEN extends ExpectedContents {
    val expectedTitle: String          = "Non-UK tax on short service refunds"
    val expectedErrorTitle             = s"Error: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedPara1: String          = "If your client paid non-UK tax on more than one refund, add the amounts together."
    val expectedPara2: String = "If you do not know this information, you can ask your client to check with the employer or pension scheme provider."
    val expectedQuestion: String                 = "Did your client pay non-UK tax on short service refunds?"
    val expectedAmountText: String               = "Total non-UK tax on short service refunds, in pounds"
    val expectedAmountHint: String               = "For example, £193.54"
    val yes: String                              = "Yes"
    val no: String                               = "No"
    val continue: String                         = "Continue"
    val errorMessage: String                     = "Select yes if your client paid non-UK tax on this short service refund"
    val expectedNoAmountEntryErrorText: String   = "Enter the amount of non-UK tax your client paid on this short service refund"
    val expectedIncorrectFormatErrorText: String = "Enter the amount of non-UK tax your client paid on this short service refund in pounds"
    val expectedTooBigErrorText: String = "The amount of non-UK tax your client paid on this short service refund must be less than £100,000,000,000"
  }

  object ExpectedContentsAgentCY extends ExpectedContents {
    val expectedTitle: String          = "Treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr"
    val expectedErrorTitle             = s"Gwall: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedPara1: String = "Os gwnaeth eich cleient dalu treth y tu allan i’r DU ar fwy nag un ad-daliad, ychwanegwch y symiau at ei gilydd."
    val expectedPara2: String = "Os nad yw’r wybodaeth hon gennych, gallwch ofyn i’ch cleient wirio gyda’r cyflogwr neu’r darparwr cynllun pensiwn."
    val expectedQuestion: String   = "A wnaeth eich cleient dalu treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr?"
    val expectedAmountText: String = "Cyfanswm treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr, mewn punnoedd"
    val expectedAmountHint: String = "For example, £193.54"
    val yes: String                = "Iawn"
    val no: String                 = "Na"
    val continue: String           = "Yn eich blaen"
    val errorMessage: String       = "Dewiswch ‘Iawn’ os gwnaeth eich cleient dalu treth y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr"
    val expectedNoAmountEntryErrorText: String =
      "Nodwch swm y dreth y gwnaeth eich cleient ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr"
    val expectedIncorrectFormatErrorText: String =
      "Nodwch swm y dreth y gwnaeth eich cleient ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr yn y fformat cywir"
    val expectedTooBigErrorText: String =
      "Mae’n rhaid i swm y dreth y gwnaeth eich cleient ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr fod yn llai na £100,000,000,000"
  }

  object ExpectedContentsIndividualCY extends ExpectedContents {
    val expectedTitle: String          = "Treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr"
    val expectedErrorTitle             = s"Gwall: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedPara1: String          = "Os gwnaethoch dalu treth y tu allan i’r DU ar fwy nag un ad-daliad, ychwanegwch y symiau at ei gilydd."
    val expectedPara2: String          = "Os nad yw’r wybodaeth hon gennych, gallwch wirio gyda’r cyflogwr neu’r darparwr cynllun pensiwn."
    val expectedQuestion: String       = "A wnaethoch dalu treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr?"
    val expectedAmountText: String     = "Cyfanswm treth y tu allan i’r DU ar ad-daliadau am wasanaeth byr, mewn punnoedd"
    val expectedAmountHint: String     = "For example, £193.54"
    val yes: String                    = "Iawn"
    val no: String                     = "Na"
    val continue: String               = "Yn eich blaen"
    val errorMessage: String           = "Dewiswch ‘Iawn’ os gwnaethoch dalu treth y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr"
    val expectedNoAmountEntryErrorText: String = "Nodwch swm y dreth y gwnaethoch ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr"
    val expectedIncorrectFormatErrorText: String =
      "Nodwch swm y dreth y gwnaethoch ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr yn y fformat cywir"
    val expectedTooBigErrorText: String =
      "Mae’n rhaid i swm y dreth y gwnaethoch ei thalu y tu allan i’r DU ar yr ad-daliad hwn am wasanaeth byr fod yn llai na £100,000,000,000"
  }

  protected val userScenarios: Seq[UserScenario[ExpectedContents, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedContentsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedContentsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedContentsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedContentsAgentCY)
  )

  private lazy val underTest = inject[NonUkTaxRefundsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "show the Non UK tax on short service refund page" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.p1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara2, Selectors.p2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedQuestion, Selectors.questionSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }
      "show the Non UK tax on short service refund page when yes is selected" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "200.00")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.p1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara2, Selectors.p2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedQuestion, Selectors.questionSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        inputFieldValueCheck("amount-2", Selectors.amountTextSelector, "200")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }
      "show the Non UK tax on short service refund page when no is selected" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "false")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara1, Selectors.p1Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedPara2, Selectors.p2Selector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedQuestion, Selectors.questionSelector)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = true)
        textOnPageCheck(userScenario.commonExpectedResults.expectedAmountText, Selectors.amountLabelSelector)
        inputFieldValueCheck("amount-2", Selectors.amountTextSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue)
        welshToggleCheck(userScenario.isWelsh)
      }
      "show an error message when page is submitted without any input" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.errorMessage, Some("value"))
        errorSummaryCheck(userScenario.commonExpectedResults.errorMessage, "#value")
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
      }
      "show an error message when page is submitted without amount input" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat                  = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.expectedNoAmountEntryErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.expectedNoAmountEntryErrorText, "#amount-2")
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
        inputFieldValueCheck("amount-2", Selectors.amountTextSelector, "")
      }
      "show an error message when page is submitted with amount in wrong format" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat = underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "123xyz")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.expectedIncorrectFormatErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.expectedIncorrectFormatErrorText, "#amount-2")
      }
      "show an error message when page is submitted that exceeds maximum amount allowed" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        def form: Form[(Boolean, Option[BigDecimal])] = new FormsProvider().nonUkTaxRefundsForm(if (userScenario.isAgent) anAgentUser else aUser)

        val htmlFormat =
          underTest(form.bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "999999999999999999999.99")), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.expectedTooBigErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.expectedTooBigErrorText, "#amount-2")
      }
    }
  }
}
