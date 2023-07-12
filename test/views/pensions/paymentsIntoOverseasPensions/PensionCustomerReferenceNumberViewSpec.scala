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

import play.api.i18n.Messages
import support.ViewUnitTest
import utils.FakeRequestProvider
//import utils.PageUrls.PaymentIntoOverseasPensions.pensionCustomerReferenceNumberUrl
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.PensionCustomerReferenceNumberForm
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.AnyContent
import views.html.pensions.paymentsIntoOverseasPensions.PensionsCustomerReferenceNumberView

class PensionCustomerReferenceNumberViewSpec extends ViewUnitTest with FakeRequestProvider {

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedParagraph1: String
    val expectedNoValueError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val expectedButtonText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your customer reference number?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedParagraph1: String = "Your pension provider should have given you a customer reference number."
    val expectedNoValueError: String = "Enter your customer reference number"
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw’ch cyfeirnod cwsmer?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedParagraph1: String = "Dylai’ch darparwr pensiwn fod wedi rhoi cyfeirnod cwsmer i chi."
    val expectedNoValueError: String = "Nodwch eich cyfeirnod cwsmer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s customer reference number?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedParagraph1: String = "Your client’s pension provider should have given them a customer reference number."
    val expectedNoValueError: String = "Enter your client’s customer reference number"

  }
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw cyfeirnod cwsmer eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedParagraph1: String = "Dylai darparwr pensiwn eich cleient fod wedi rhoi cyfeirnod cwsmer i’ch cleient."
    val expectedNoValueError: String = "Nodwch gyfeirnod cwsmer eich cleient"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText: String = "For example, 'PENSIONINCOME245'"
    val expectedButtonText: String = "Continue"

  }
  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText: String = "Er enghraifft, 'INCWMPENSIWN245'"
    val expectedButtonText: String = "Yn eich blaen"
  }

  val inputName: String = "pensionsCustomerReferenceNumberId"

  def pensionCustomerReferenceNumberUrl(taxYear: Int, index: Int): String =
    s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-customer-reference-number?index=$index"

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#pensionsCustomerReferenceNumberId"
    val hintTextSelector = "#pensionsCustomerReferenceNumberId-hint"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  private lazy val underTest = inject[PensionsCustomerReferenceNumberView]

  userScenarios.foreach { userScenario =>

    import Selectors._
    def form: Form[String] =
      PensionCustomerReferenceNumberForm.pensionCustomerReferenceNumberForm(userScenario.specificExpectedResults.get.expectedNoValueError)

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form, taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.hintText, hintTextSelector)
        inputFieldValueCheck(inputName, inputSelector, "")
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, continueButtonSelector)
        formPostLinkCheck(pensionCustomerReferenceNumberUrl(taxYearEOY, 0), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with pre-filled data" which {

        val pensionsCustomerReferenceNumber = "PENSIONSINCOME245"

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form.fill(pensionsCustomerReferenceNumber), taxYearEOY, Some(1))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.hintText, hintTextSelector)
        inputFieldValueCheck(inputName, inputSelector, "PENSIONSINCOME245")
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, continueButtonSelector)
        formPostLinkCheck(pensionCustomerReferenceNumberUrl(taxYearEOY, 1), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when no data is submitted" which {

        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = {
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        }

        val htmlFormat = underTest(form.bind(Map("incorrectFormatMsg" -> "")), taxYearEOY, Some(0))
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoValueError, inputSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoValueError, Some("pensionsCustomerReferenceNumberId"))
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
      }
    }
  }
}
