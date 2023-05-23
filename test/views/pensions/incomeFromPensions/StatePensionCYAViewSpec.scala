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
import controllers.pensions.incomeFromPensions.routes
import models.pension.statebenefits.IncomeFromPensionsViewModel
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.incomeFromPensions.StatePensionCYAView

class StatePensionCYAViewSpec extends ViewUnitTest { //scalastyle:off magic.number

  object ChangeLinks {
    val changeStatePension = () => routes.StatePensionController.show(taxYearEOY).url
    val changeStartDate = () => routes.StatePensionStartDateController.show(taxYearEOY).url
    val changeLumpSum = () => routes.StatePensionLumpSumController.show(taxYearEOY).url
    val changeLumpSumTax = () => routes.TaxPaidOnStatePensionLumpSumController.show(taxYearEOY).url
    val changeLumpSumStartDate = () => routes.StatePensionLumpSumStartDateController.show(taxYearEOY).url
    val changeAddStatePensionToTaxCalcs =
      () => routes.StatePensionCYAController.show(taxYearEOY).url  //TODO:change to AddStatePensionToTaxCalcs route
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val statePension: String
    val hiddenStatePension: String
    val statePensionValue : String
    val pensionStartDate: String
    val hiddenPensionStartDate: String
    val pensionStartDateValue: String
    val lumpSum: String
    val hiddenLumpSum: String
    val lumpSumValue: String
    val lumpSumTax: String
    val hiddenLumpSumTax: String
    val lumpSumTaxValue: String
    val lumpSumDate: String
    val hiddenLumpSumDate: String
    val lumpSumDateValue: String
    val addStatePensionToTaxCalcs: String
    val hiddenAddStatePensionToTaxCalcs: String
    val addStatePensionToTaxCalcsAnswer: String
    
    val buttonText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val statePension: String = "State Pension"
    val hiddenStatePension = "Change state pension"
    val statePensionValue : String = "£155.88"
    val pensionStartDate: String = "State Pension start date"
    val hiddenPensionStartDate: String = "Change State Pension start date"
    val pensionStartDateValue: String = "13 November 2019"
    val lumpSum = "Lump sum"
    val hiddenLumpSum = "Change lump sum"
    val lumpSumValue: String = "£166.88"
    val lumpSumTax: String = "Lump sum tax"
    val hiddenLumpSumTax: String = "Change Lump sum tax"
    val lumpSumTaxValue: String = "£3.99"
    val lumpSumDate: String = "Lump sum date"
    val hiddenLumpSumDate: String = "Change lump sum date"
    val lumpSumDateValue: String = "14 November 2019"
    val addStatePensionToTaxCalcs: String = "State Pension added to tax calculation"
    val hiddenAddStatePensionToTaxCalcs: String = "Change State Pension added to tax calculation"
    val addStatePensionToTaxCalcsAnswer: String = "Yes"
    val buttonText: String = "Save and continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    
    val statePension: String = "State Pension"
    val hiddenStatePension = "Change state pension"
    val statePensionValue : String = "£155.88"
    val pensionStartDate: String = "State Pension start date"
    val hiddenPensionStartDate: String = "Change State Pension start date"
    val pensionStartDateValue: String = "13 November 2019"
    val lumpSum = "Lump sum"
    val hiddenLumpSum = "Change lump sum"
    val lumpSumValue: String = "£166.88"
    val lumpSumTax: String = "Lump sum tax"
    val hiddenLumpSumTax: String = "Change Lump sum tax"
    val lumpSumTaxValue: String = "£3.99"
    val lumpSumDate: String = "Lump sum date"
    val hiddenLumpSumDate: String = "Change lump sum date"
    val lumpSumDateValue: String = "14 November 2019"
    val addStatePensionToTaxCalcs: String = "State Pension added to tax calculation"
    val hiddenAddStatePensionToTaxCalcs: String = "Change State Pension added to tax calculation"
    val addStatePensionToTaxCalcsAnswer: String = "Iawn"
    val buttonText: String = "Cadw ac yn eich blaen"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
  }
  
  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Check your State Pension"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Check your clients State Pension"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Check your State Pension"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Check your clients State Pension"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  lazy val underTest = inject[StatePensionCYAView]

  private def renderPage(userScenario: UserScenario[CommonExpectedResults, SpecificExpectedResults], model: IncomeFromPensionsViewModel) = {
    implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
    implicit val messages: Messages = getMessages(userScenario.isWelsh)

    val htmlFormat = underTest(taxYearEOY, model)
    implicit val document: Document = Jsoup.parse(htmlFormat.body)

    userScenario.specificExpectedResults.foreach { spER =>
      titleCheck(spER.expectedTitle, userScenario.isWelsh)
      h1Check(spER.expectedHeading)
    }
    captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
    document
  }


  userScenarios.foreach { userScenario =>
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      
      "render the CYA page when answering State Pension = No, Lump Sum = No, Adding State Pension to tax calculations= No" when {
        val viewModel = anIncomeFromPensionsViewModel.copy(
          statePension = anIncomeFromPensionsViewModel
            .statePension.map(_.copy(amountPaidQuestion = Some(false), taxPaidQuestion = Some(false))),
          statePensionLumpSum = anIncomeFromPensionsViewModel
            .statePensionLumpSum.map(_.copy(amountPaidQuestion = Some(false), taxPaidQuestion = Some(false)))
          //todo: add conditions for adding state pensions to tax calculations
        )
        implicit val document: Document = renderPage(userScenario, viewModel)

        cyaRowCheck(statePension, statePensionValue, ChangeLinks.changeStatePension(),hiddenStatePension,1)
        cyaRowCheck(lumpSum, lumpSumValue, ChangeLinks.changeLumpSum(),hiddenLumpSum,2)
        cyaRowCheck(addStatePensionToTaxCalcs, addStatePensionToTaxCalcsAnswer, ChangeLinks.changeAddStatePensionToTaxCalcs(),
                                                                       hiddenAddStatePensionToTaxCalcs,3)
        buttonCheck(buttonText)
      }

      "render the CYA page when answering State Pension = Yes, Lump Sum = No, Adding State Pension to tax calculations= No" when {
        val viewModel = anIncomeFromPensionsViewModel.copy(
          statePension = anIncomeFromPensionsViewModel
            .statePension.map(_.copy(amountPaidQuestion = Some(true), taxPaidQuestion = Some(false))),
          statePensionLumpSum = anIncomeFromPensionsViewModel
            .statePensionLumpSum.map(_.copy(amountPaidQuestion = Some(false), taxPaidQuestion = Some(false)))
          //todo: add conditions for adding state pensions to tax calculations
        )
        implicit val document: Document = renderPage(userScenario, viewModel)

        cyaRowCheck(statePension, statePensionValue, ChangeLinks.changeStatePension(), hiddenStatePension, 1)
        cyaRowCheck(pensionStartDate, pensionStartDateValue, ChangeLinks.changeStartDate(),hiddenPensionStartDate,2)
        cyaRowCheck(lumpSum, lumpSumValue, ChangeLinks.changeLumpSum(), hiddenLumpSum, 3)
        cyaRowCheck(addStatePensionToTaxCalcs, addStatePensionToTaxCalcsAnswer, ChangeLinks.changeAddStatePensionToTaxCalcs(),
                                                                                hiddenAddStatePensionToTaxCalcs, 4)
        buttonCheck(buttonText)
      }

      "render the CYA page when answering State Pension = Yes, Lump Sum = Yes, Lump sum tax = No, Adding State Pension to tax calculations = No" when {
        val viewModel = anIncomeFromPensionsViewModel.copy(
          statePension = anIncomeFromPensionsViewModel
            .statePension.map(_.copy(amountPaidQuestion = Some(true), taxPaidQuestion = Some(false))),
          statePensionLumpSum = anIncomeFromPensionsViewModel
            .statePensionLumpSum.map(_.copy(amountPaidQuestion = Some(true), taxPaidQuestion = Some(false)))
          //todo: add conditions for adding state pensions to tax calculations
        )
        implicit val document: Document = renderPage(userScenario, viewModel)

        cyaRowCheck(statePension, statePensionValue, ChangeLinks.changeStatePension(), hiddenStatePension, 1)
        cyaRowCheck(pensionStartDate, pensionStartDateValue, ChangeLinks.changeStartDate(), hiddenPensionStartDate, 2)
        cyaRowCheck(lumpSum, lumpSumValue, ChangeLinks.changeLumpSum(), hiddenLumpSum, 3)
        cyaRowCheck(lumpSumTax, lumpSumTaxValue, ChangeLinks.changeLumpSumTax(), hiddenLumpSumTax, 4)
        cyaRowCheck(lumpSumDate, lumpSumDateValue, ChangeLinks.changeLumpSumStartDate(), hiddenLumpSumDate, 5)
        cyaRowCheck(addStatePensionToTaxCalcs, addStatePensionToTaxCalcsAnswer, ChangeLinks.changeAddStatePensionToTaxCalcs(),
          hiddenAddStatePensionToTaxCalcs, 6)
        buttonCheck(buttonText)
      }
      "render the CYA page when answering State Pension = Yes, Lump Sum = Yes, Lump sum tax = Yes, Adding State Pension to tax calculations = No" when {
        val viewModel = anIncomeFromPensionsViewModel.copy(
          statePension = anIncomeFromPensionsViewModel
            .statePension.map(_.copy(amountPaidQuestion = Some(true), taxPaidQuestion = Some(false))),
          statePensionLumpSum = anIncomeFromPensionsViewModel
            .statePensionLumpSum.map(_.copy(amountPaidQuestion = Some(true), taxPaidQuestion = Some(true)))
          //todo: add conditions for adding state pensions to tax calculations
        )
        implicit val document: Document = renderPage(userScenario, viewModel)

        cyaRowCheck(statePension, statePensionValue, ChangeLinks.changeStatePension(), hiddenStatePension, 1)
        cyaRowCheck(pensionStartDate, pensionStartDateValue, ChangeLinks.changeStartDate(), hiddenPensionStartDate, 2)
        cyaRowCheck(lumpSum, lumpSumValue, ChangeLinks.changeLumpSum(), hiddenLumpSum, 3)
        cyaRowCheck(lumpSumTax, lumpSumTaxValue, ChangeLinks.changeLumpSumTax(), hiddenLumpSumTax, 4)
        cyaRowCheck(lumpSumDate, lumpSumDateValue, ChangeLinks.changeLumpSumStartDate(), hiddenLumpSumDate, 5)
        cyaRowCheck(addStatePensionToTaxCalcs, addStatePensionToTaxCalcsAnswer, ChangeLinks.changeAddStatePensionToTaxCalcs(),
          hiddenAddStatePensionToTaxCalcs, 6)
        buttonCheck(buttonText)
      }
    }
  }
}
