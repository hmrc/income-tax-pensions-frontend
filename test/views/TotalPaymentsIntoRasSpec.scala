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

import controllers.pensions.paymentsIntoPensions.PaymentsIntoPensionFormProvider
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.TotalPaymentsIntoRASView
import views.TotalPaymentsIntoRasSpec._

object TotalPaymentsIntoRasSpec {

  val oneOffAmount: String = "£1,400"
  val rasTotal: String = "£8,800"

  val calculatedRAS: String = "£7,040"
  val calculatedRelief: String = "£1,760"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val pSelector = "#main-content > div > div > p"
    val isCorrectSelector = "#main-content > div > div > form > div > fieldset > legend"
    val tableSelector: (Int, Int) => String = (row, column) =>
      s"#main-content > div > div > table > tbody > tr:nth-child($row) > td:nth-of-type($column)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedParagraph: String
    val expectedErrorTitle: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val totalPayments: String
    val oneOff: String
    val claimed: String
    val total: String
    val isCorrect: String
    val expectedError: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Your total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount you paid, plus basic rate tax relief, is $rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Cyfanswm eich taliadau i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS)"
    val expectedParagraph: String = s"Y cyfanswm a dalwyd gennych, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, yw $rasTotal. " +
      "Gallwch ddod o hyd i’r ffigur hwn ar y dystysgrif pensiwn neu’r dderbynneb gan eich gweinyddwr."
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Your client’s total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount your client paid, plus basic rate tax relief, is $rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your client’s administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Cyfanswm taliadau eich cleient i mewn i bensiynau rhyddhad wrth y ffynhonnell (RAS)"
    val expectedParagraph: String = s"Y cyfanswm a dalwyd gan eich cleient, ynghyd â rhyddhad treth ar y gyfradd sylfaenol, yw $rasTotal. " +
      "Gallwch ddod o hyd i’r ffigur hwn ar y dystysgrif pensiwn neu’r dderbynneb gan weinyddwr eich cleient."
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val totalPayments: String = "Total pension payments"
    val oneOff: String = s"(Including $oneOffAmount one-off payments)"
    val claimed: String = "Tax relief claimed by scheme"
    val total: String = "Total"
    val isCorrect: String = "Is this correct?"
    val expectedError: String = "Select yes if the figures are correct"
    val expectedButtonText: String = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val totalPayments: String = "Cyfanswm y taliadau pensiwn"
    val oneOff: String = s"(Gan gynnwys $oneOffAmount o daliadau untro)"
    val claimed: String = "Rhyddhad treth a hawliwyd gan y cynllun"
    val total: String = "Cyfanswm"
    val isCorrect: String = "A yw hyn yn gywir?"
    val expectedError: String = "Dewiswch ‘Iawn’ os yw’r ffigurau’n gywir"
    val expectedButtonText: String = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }
}

class TotalPaymentsIntoRasSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form = new PaymentsIntoPensionFormProvider().totalPaymentsIntoRASForm

  private lazy val underTest = inject[TotalPaymentsIntoRASView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      import Selectors._
      import userScenario.commonExpectedResults._
      
      "render 'Total payments into RAS pensions' page with correct content and no pre-filling" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val htmlFormat = underTest(form, taxYearEOY, rasTotal, Some(oneOffAmount), calculatedRAS, calculatedRelief)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedTitle)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, pSelector)
        textOnPageCheck(isCorrect, isCorrectSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(totalPaymentsIntoRASUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(s"$totalPayments $oneOff", tableSelector(1, 1))
        textOnPageCheck(s"$calculatedRAS", tableSelector(1, 2))
        textOnPageCheck(claimed, tableSelector(2, 1))
        textOnPageCheck(s"$calculatedRelief", tableSelector(2, 2))
        textOnPageCheck(total, tableSelector(3, 1))
        textOnPageCheck(s"$rasTotal", tableSelector(3, 2))
      }

      "render 'Total payments into RAS pensions' page without a one-off amount" should {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val htmlFormat = underTest(form, taxYearEOY, rasTotal, None, calculatedRAS, calculatedRelief)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        textOnPageCheck(s"${userScenario.commonExpectedResults.totalPayments}", Selectors.tableSelector(1, 1))
      }

      "render the page with the radio button pre-filled" should {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val htmlFormat = underTest(form.fill(true), taxYearEOY, rasTotal, Some(oneOffAmount), calculatedRAS, calculatedRelief)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        "have the yes button pre-filled" when {
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
        }
      }

      "return an error when form is submitted with no entry" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, rasTotal, Some(oneOffAmount), calculatedRAS, calculatedRelief)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedTitle)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, pSelector)
        textOnPageCheck(isCorrect, isCorrectSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(totalPaymentsIntoRASUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(s"$totalPayments $oneOff", tableSelector(1, 1))
        textOnPageCheck(s"$calculatedRAS", tableSelector(1, 2))
        textOnPageCheck(claimed, tableSelector(2, 1))
        textOnPageCheck(s"$calculatedRelief", tableSelector(2, 2))
        textOnPageCheck(total, tableSelector(3, 1))
        textOnPageCheck(s"$rasTotal", tableSelector(3, 2))

        errorSummaryCheck(expectedError, Selectors.yesSelector)
        errorAboveElementCheck(expectedError, Some("value"))
      }
    }
  }
}
