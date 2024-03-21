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

package views.pensions.paymentsIntoOverseasPensions

import controllers.pensions.paymentsIntoOverseasPensions.routes.UntaxedEmployerPaymentsController
import forms.{AmountForm, FormsProvider}
import models.pension.pages.UntaxedEmployerPayments
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.twirl.api.Html
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.UntaxedEmployerPaymentsView

class UntaxedEmployerControllerSpec extends ViewUnitTest with FakeRequestProvider {

  private lazy val underTest = inject[UntaxedEmployerPaymentsView]
  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )
  val poundPrefixText = "£"
  val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedPara2: String
    val expectedSub1Para1: String
    val expectedSub2Para1: String
    val expectedSub2Details1: String
    val expectedSub2Details2: String
    val expectedSub2Details3: String
    val expectedQuestion: String
    val expectedErrorNoEntry: String
    val expectedErrorTooBig: String
    val expectedErrorInvalidFormat: String
  }

  trait CommonExpectedResults {
    lazy val expectedHeading: String = expectedTitle
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedPara1: String
    val expectedSubHeading1: String
    val expectedSubHeading2: String
    val expectedDetailsHeading: String
    val expectedCaption: Int => String
    val expectedButtonText: String
    val hintText: String
  }

  object Selectors {

    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val findOutLinkSelector            = "#annual-allowance-link"
    val overLimitLinkSelector          = "#over-limit-link"
    val detailsSelector                = "#main-content > div > div > form > details > summary > span"

    val sub1Selector      = "#main-content > div > div > div:nth-child(4) > h2"
    val sub1SelectorPara1 = "#main-content > div > div > div:nth-child(4) > p"
    val sub2Selector      = "#main-content > div > div > div:nth-child(5) > h2"
    val sub2SelectorPara1 = "#main-content > div > div > div:nth-child(5) > p"
    val detailsTitle      = "#main-content > div > div > div:nth-child(5) > details > summary > span"

    val questionSelector = "#main-content > div > div > form > div > label"

    val hintTextSelector    = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector       = "#amount"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > div:nth-child(5) > details > div > ol > > li:nth-child($index)"

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedPara2: String        = "You need to know which type of scheme your employer paid into."
    override val expectedSub1Para1: String    = "If you have this type of scheme, tell us the total amount your employers paid in."
    override val expectedSub2Para1: String    = "If you have this type of scheme, tell us the value of the amount your employers paid in."
    override val expectedSub2Details1: String = "how much your lump sum went up by"
    override val expectedSub2Details2: String = "plus 16 times how much your annual pension went up by"
    override val expectedSub2Details3: String = "minus any payments you made into the scheme"
    override val expectedQuestion: String     = "How much did your employers pay into your overseas pension scheme?"
    override val expectedErrorNoEntry: String = "Enter the amount your employers paid into your overseas pension schemes"
    override val expectedErrorTooBig: String  = "The amount your employers paid into overseas pension schemes must be less than £100,000,000,000"
    override val expectedErrorInvalidFormat: String = "Enter the amount your employers paid into overseas pension schemes in pounds"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedPara2: String     = "Mae angen i chi wybod pa fath o gynllun y talodd eich cyflogwr i mewn iddo."
    override val expectedSub1Para1: String = "Os oes gennych y math hwn o gynllun, rhowch wybod i ni’r cyfanswm a dalwyd i mewn gan eich cyflogwyr."
    override val expectedSub2Para1: String = "Os oes gennych y math hwn o gynllun, rhowch wybod i ni gwerth y swm a dalwyd i mewn gan eich cyflogwyr."
    override val expectedSub2Details1: String = "faint aeth eich cyfandaliad i fyny"
    override val expectedSub2Details2: String = "ynghyd ag 16 gwaith faint yr aeth eich pensiwn blynyddol i fyny"
    override val expectedSub2Details3: String = "llai unrhyw daliadau a wnaethoch i mewn i’r cynllun"
    override val expectedQuestion: String     = "Faint dalodd eich cyflogwyr i mewn i’ch cynllun pensiwn tramor?"
    override val expectedErrorNoEntry: String = "Nodwch y swm y dalodd eich cyflogwyr i mewn i’ch cynlluniau pensiwn tramor"
    override val expectedErrorTooBig: String  = "The amount your employers paid into overseas pension schemes must be less than £100,000,000,000"
    override val expectedErrorInvalidFormat: String = "Nodwch y swm a dalodd eich cyflogwyr i mewn i gynlluniau pensiwn tramor yn y fformat cywir"

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedPara2: String        = "You need to know which type of scheme your client’s employer paid into."
    val expectedSub1Para1: String    = "If your client has this type of scheme, tell us the total amount their employers paid in."
    val expectedSub2Para1: String    = "If your client has this type of scheme, tell us the value of the amount their employers paid in."
    val expectedSub2Details1: String = "how much your client’s lump sum went up by"
    val expectedSub2Details2: String = "plus 16 times how much your client’s annual pension went up by"
    val expectedSub2Details3: String = "minus any payments your client made into the scheme"
    val expectedQuestion: String     = "How much did your client’s employers pay into the overseas pension scheme?"
    val expectedErrorNoEntry: String = "Enter the amount that your client’s employer paid into their overseas pension scheme"
    val expectedErrorTooBig: String =
      "The total amount that your client’s employers paid into their overseas pension scheme must be less than £100,000,000,000"
    val expectedErrorInvalidFormat: String = "Enter the total amount that your client’s employers paid into their overseas pension scheme in pounds"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedPara2: String = "Mae angen i chi wybod pa fath o gynllun y talodd cyflogwr eich cleient i mewn iddo."
    val expectedSub1Para1: String =
      "Os oes gan eich cleient y math hwn o gynllun, rhowch wybod i ni’r cyfanswm a dalwyd i mewn gan gyflogwyr eich cleient."
    val expectedSub2Para1: String =
      "Os oes gan eich cleient y math hwn o gynllun, rhowch wybod i ni gwerth y swm a dalwyd i mewn gan gyflogwyr eich cleient."
    val expectedSub2Details1: String = "faint aeth cyfandaliad eich cleient i fyny"
    val expectedSub2Details2: String = "ynghyd ag 16 gwaith faint yr aeth pensiwn blynyddol eich cleient i fyny"
    val expectedSub2Details3: String = "llai unrhyw daliadau a wnaeth eich cleient i mewn i’r cynllun"
    val expectedQuestion: String     = "Faint dalodd cyflogwyr eich cleient i mewn i’r cynlluniau pensiwn tramor?"
    val expectedErrorNoEntry: String = "Nodwch y swm y dalodd cyflogwr eich cleient i mewn i’w gynllun pensiwn tramor"
    val expectedErrorTooBig: String =
      "The total amount that your client’s employers paid into their overseas pension scheme must be less than £100,000,000,000"
    val expectedErrorInvalidFormat: String =
      "Nodwch y cyfanswm a dalodd cyflogwyr eich cleient i mewn i gynllun pensiwn tramor eich cleient yn y fformat cywir"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText             = "Continue"
    val hintText: String               = "For example, £193.52"
    val expectedTitle: String          = "Untaxed employer payments"
    val expectedErrorTitle: String     = s"Error: $expectedTitle"
    val expectedPara1: String          = "This is also known as exempt employers’ contributions."
    val expectedSubHeading1: String    = "Contribution schemes (money purchase schemes)"
    val expectedSubHeading2: String    = "Benefits schemes (average or final salary schemes)"
    val expectedDetailsHeading: String = "Work out the value of payments into a benefits scheme"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText             = "Yn eich blaen"
    val hintText: String               = "Er enghraifft, £193.52"
    val expectedTitle: String          = "Taliadau cyflogwr sydd heb eu trethu"
    val expectedErrorTitle: String     = s"Gwall: $expectedTitle"
    val expectedPara1: String          = "Enw arall ar hyn yw cyfraniadau cyflogwr sydd wedi eu heithrio."
    val expectedSubHeading1: String    = "Cynlluniau cyfraniadau (cynlluniau prynu arian)"
    val expectedSubHeading2: String    = "Cynlluniau buddiannau (cynlluniau cyfartaledd cyflog neu gyflog terfynol)"
    val expectedDetailsHeading: String = "Cyfrifwch werth y taliadau i mewn i gynllun buddiannau"
  }
  userScenarios.foreach { user =>
    import Selectors._
    import user.commonExpectedResults._

    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "render untaxed employment payments without pre filled date" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(user.isAgent)
        implicit val messages: Messages                                         = getMessages(user.isWelsh)

        val form                        = new FormsProvider().untaxedEmployerPayments(user.isAgent)
        val pageModel                   = UntaxedEmployerPayments(taxYear, Some(0), form)
        implicit val htmlFormat: Html   = underTest(pageModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, user.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYear), captionSelector)
        textOnPageCheck(expectedPara1, paragraphSelector(1))
        textOnPageCheck(user.specificExpectedResults.get.expectedPara2, paragraphSelector(2))
        textOnPageCheck(expectedSubHeading1, sub1Selector)
        textOnPageCheck(user.specificExpectedResults.get.expectedSub1Para1, sub1SelectorPara1)
        textOnPageCheck(expectedSubHeading2, sub2Selector)
        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Para1, sub2SelectorPara1)
        textOnPageCheck(expectedDetailsHeading, detailsTitle)
        // TODO need to investigate and fix how changing joda time to java time has affected this test
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details1, detailsBulletSelector(1))
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details2, detailsBulletSelector(2))
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details3, detailsBulletSelector(3))
        textOnPageCheck(user.specificExpectedResults.get.expectedQuestion, questionSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UntaxedEmployerPaymentsController.submit(taxYear, Some(0)).url, formSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render untaxed employment payments with pre filled data" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(user.isAgent)
        implicit val messages: Messages                                         = getMessages(user.isWelsh)

        val form                        = new FormsProvider().untaxedEmployerPayments(user.isAgent)
        val pageModel                   = UntaxedEmployerPayments(taxYear, Some(0), form.fill(999.98))
        implicit val htmlFormat: Html   = underTest(pageModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, user.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYear), captionSelector)
        textOnPageCheck(expectedPara1, paragraphSelector(1))
        textOnPageCheck(user.specificExpectedResults.get.expectedPara2, paragraphSelector(2))
        textOnPageCheck(expectedSubHeading1, sub1Selector)
        textOnPageCheck(user.specificExpectedResults.get.expectedSub1Para1, sub1SelectorPara1)
        textOnPageCheck(expectedSubHeading2, sub2Selector)
        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Para1, sub2SelectorPara1)
        textOnPageCheck(expectedDetailsHeading, detailsTitle)
        // TODO need to investigate and fix how changing joda time to java time has affected this test
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details1, detailsBulletSelector(1))
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details2, detailsBulletSelector(2))
        //        textOnPageCheck(user.specificExpectedResults.get.expectedSub2Details3, detailsBulletSelector(3))
        textOnPageCheck(user.specificExpectedResults.get.expectedQuestion, questionSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "999.98")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UntaxedEmployerPaymentsController.submit(taxYear, Some(0)).url, formSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render untaxed employment payments with an error when the user doesn’t input an amount" which {
        implicit val messages: Messages                                         = getMessages(user.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(user.isAgent)

        val form                        = new FormsProvider().untaxedEmployerPayments(user.isAgent)
        val pageModel                   = UntaxedEmployerPayments(taxYear, Some(0), form.bind(Map(AmountForm.amount -> "")))
        implicit val htmlFormat: Html   = underTest(pageModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, user.isWelsh)
        h1Check(expectedHeading)
        errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some("amount"))
        errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, "#amount")
      }

      "render untaxed employment payments with an error when amount is in wrong format" which {
        implicit val messages: Messages                                         = getMessages(user.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(user.isAgent)

        val form                        = new FormsProvider().untaxedEmployerPayments(user.isAgent)
        val pageModel                   = UntaxedEmployerPayments(taxYear, Some(0), form.bind(Map(AmountForm.amount -> "incorrect-format")))
        implicit val htmlFormat: Html   = underTest(pageModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, user.isWelsh)
        h1Check(expectedHeading)
        errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorInvalidFormat, Some("amount"))
        errorSummaryCheck(user.specificExpectedResults.get.expectedErrorInvalidFormat, "#amount")
      }
      "render untaxed employment payments with an error when the user selects yes but amount exceeds max" which {
        implicit val messages: Messages                                         = getMessages(user.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(user.isAgent)

        val form                        = new FormsProvider().untaxedEmployerPayments(user.isAgent)
        val pageModel                   = UntaxedEmployerPayments(taxYear, Some(0), form.bind(Map(AmountForm.amount -> "1000000000000000000000.00")))
        implicit val htmlFormat: Html   = underTest(pageModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, user.isWelsh)
        h1Check(expectedHeading)
        errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorTooBig, Some("amount"))
        errorSummaryCheck(user.specificExpectedResults.get.expectedErrorTooBig, "#amount")
      }
    }
  }
}
