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

package views.pensions.annualAllowances

import forms.FormsProvider.pensionProviderPaidTaxForm
import forms.RadioButtonAmountForm
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.annualAllowances.PensionProviderPaidTaxView


class PensionProviderPaidTaxViewSpec extends ViewUnitTest {
  
  object Selectors {
    val amountHeadingSelector: String = "#conditional-value > div > label"
    val amountHintSelector: String =  "#amount-2-hint"
    val amountValueSelector: String =  "#amount-2"
  }
  
  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val amountText: String
    val amountHint: String
    val yesText: String
    val noText: String
    val continue: String
    val errorAmountIdOpt: String
    val errorZeroAmount: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = taxYear => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val amountText = "Amount they paid or agreed to pay, in pounds"
    val amountHint = "For example, £193.52"
    val yesText = "Yes"
    val noText = "No"
    val continue = "Continue"
    val errorAmountIdOpt = "amount-2"
    val errorZeroAmount = "Enter an amount greater than zero"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = taxYear => s"Lwfans blynyddol ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val amountText ="Y swm a dalwyd ganddo, neu’r swm a gytunodd i’w dalu, mewn punnoedd"
    val amountHint = "Er enghraifft, £193.52"
    val yesText = "Iawn"
    val noText = "Na"
    val continue = "Yn eich blaen"
    val errorAmountIdOpt = "amount-2"
    val errorZeroAmount = "Nodwch swm sy’n fwy na sero"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val errorTitle: String
    lazy val expectedH1: String = expectedTitle
    val expectedErrorSelectText: String
    val errorEmptyAmountText: String
    val errorAmountFormatText: String
    val errorAmountLessThan: String
  }
  
  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    lazy val errorTitle = s"Error: $expectedTitle"
    val expectedErrorSelectText = "Select yes if your pension schemes paid or agreed to pay tax"
    val errorEmptyAmountText = "Enter the amount of tax your pension schemes paid or agreed to pay"
    val errorAmountFormatText = "Enter the amount of tax your pension schemes paid, or agreed to pay, in pounds and pence"
    val errorAmountLessThan = "The amount of tax your pension schemes paid or agreed to pay must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s pension schemes pay or agree to pay the tax?"
    lazy val errorTitle = s"Error: $expectedTitle"
    val expectedErrorSelectText = "Select yes if your client’s pension provider paid or agreed to pay the tax"
    val errorEmptyAmountText = "Enter the amount of tax your client’s pension provider paid or agreed to pay"
    val errorAmountFormatText = "Enter the amount of tax your client’s pension provider paid, or agreed to pay, in pounds and pence"
    val errorAmountLessThan = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cynlluniau pensiwn dalu’r dreth neu gytuno i dalu’r dreth?"
    lazy val errorTitle = s"Gwall: $expectedTitle"
    val expectedErrorSelectText = "Dewiswch ‘Iawn’ os gwnaeth eich darparwr pensiwn dalu treth neu gytuno i wneud hynny"
    val errorEmptyAmountText = s"Nodwch swm y dreth a dalwyd gan eich darparwr pensiwn, neu’r swm a gytunodd i’w dalu"
    val errorAmountFormatText = s"Nodwch swm y dreth a dalwyd gan eich darparwr pensiwn, neu’r swm a gytunodd i’w dalu, yn y fformat cywir"
    val errorAmountLessThan = "Mae’n rhaid i swm y dreth a dalwyd gan eich darparwr pensiwn, neu’r swm a gytunodd i’w dalu, fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth cynlluniau pensiwn eich cleient dalu’r dreth neu gytuno i wneud hynny?"
    lazy val errorTitle = s"Gwall: $expectedTitle"
    val expectedErrorSelectText = "Dewiswch ‘Iawn’ os gwnaeth darparwr pensiwn eich cleient dalu treth neu gytuno i wneud hynny"
    val errorEmptyAmountText = "Nodwch swm y dreth a dalwyd gan ddarparwr pensiwn eich cleient, neu’r swm a gytunodd i’w dalu"
    val errorAmountFormatText = "Nodwch swm y dreth a dalwyd gan ddarparwr pensiwn eich cleient, neu’r swm a gytunodd i’w dalu, yn y fformat cywir"
    val errorAmountLessThan = "Mae’n rhaid i swm y dreth a dalwyd gan ddarparwr pensiwn eich cleient, neu’r swm a gytunodd i’w dalu, fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[PensionProviderPaidTaxView]
  
  userScenarios.foreach { userScenario =>

    def checkCommonElements(userScenario: UserScenario[CommonExpectedResults, SpecificExpectedResults])
                           (implicit document: Document): Unit = {
      titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
      h1Check(userScenario.specificExpectedResults.get.expectedH1)
      captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
    }

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {  //scalastyle:off magic.number

      "render page with no prefilled data" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val formData = pensionProviderPaidTaxForm(userScenario.isAgent)

        val htmlFormat = underTest(formData, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)
        radioButtonCheck(userScenario.commonExpectedResults.yesText,1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.noText,2, checked = false)
        buttonCheck(continue)
        welshToggleCheck(userScenario.isWelsh)
      }
      
      "render the page with a radio choice Yes selected and an amount is submitted" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        
        val formData = pensionProviderPaidTaxForm(userScenario.isAgent)
          .bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "20"))
        
        val htmlFormat = underTest(formData,taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.noText, 2, checked = false)
        textOnPageCheck(userScenario.commonExpectedResults.amountText, Selectors.amountHeadingSelector)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, Selectors.amountHintSelector)
        inputFieldValueCheck(userScenario.commonExpectedResults.errorAmountIdOpt, Selectors.amountValueSelector, "20")
        buttonCheck(continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the page with a radio choice No" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        
        val formData = pensionProviderPaidTaxForm(userScenario.isAgent)
          .bind(Map(RadioButtonAmountForm.yesNo -> "false"))
        
        val htmlFormat = underTest(formData, taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)
        radioButtonCheck(userScenario.commonExpectedResults.yesText, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.noText, 2, checked = true)
        buttonCheck(continue)
        welshToggleCheck(userScenario.isWelsh)
      }

      for (errorType <- Seq("empty", "incorrect format", "max amount", "zero amount")) {
        s"render the page with a radio choice Yes selected and $errorType amount is submitted" which {
          implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val bindAmountData = errorType match {
            case "empty" => ""
            case "incorrect format" => "123.45678"
            case "max amount" => "200000000000"
            case "zero amount" => "0"
          }
          val formData = pensionProviderPaidTaxForm(userScenario.isAgent)
            .bind(Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> bindAmountData))

          val htmlFormat = underTest(formData, taxYearEOY)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.errorTitle, userScenario.isWelsh)
          
          errorType match {
            case "empty" =>
              errorSummaryCheck(userScenario.specificExpectedResults.get.errorEmptyAmountText, Selectors.amountValueSelector)
              errorAboveElementCheck(userScenario.specificExpectedResults.get.errorEmptyAmountText)
            case "incorrect format" =>
              errorSummaryCheck(userScenario.specificExpectedResults.get.errorAmountFormatText, Selectors.amountValueSelector)
              errorAboveElementCheck(userScenario.specificExpectedResults.get.errorAmountFormatText)
            case "max amount" =>
              errorSummaryCheck(userScenario.specificExpectedResults.get.errorAmountLessThan, Selectors.amountValueSelector)
              errorAboveElementCheck(userScenario.specificExpectedResults.get.errorAmountLessThan)
            case "zero amount" =>
              errorSummaryCheck(userScenario.commonExpectedResults.errorZeroAmount, Selectors.amountValueSelector)
              errorAboveElementCheck(userScenario.commonExpectedResults.errorZeroAmount)
          }
        }
      }
    }
  }
}

