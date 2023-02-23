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

import builders.PensionsUserDataBuilder.aPensionsUserData
import models.requests.UserSessionDataRequest
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.WorkplacePensionControllerSpec._
import controllers.pensions.incomeFromOverseasPensions.routes
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import models.pension.pages.OverseasPensionSchemeSummaryPage
import org.jsoup.Jsoup
import views.html.pensions.incomeFromOverseasPensions.PensionsSchemeSummary


class OverseasPensionSchemeSummarySpec extends ViewUnitTest {

  object selectors {
    def getRowKey(index: Int) = s"#main-content > div > div > dl > div:nth-child($index) > dt"
    def getRowValue(index: Int) = s"#main-content > div > div > dl > div:nth-child($index) > dd.govuk-summary-list__value"
    def getRowChange(index: Int) = s"#main-content > div > div > dl > div:nth-child($index) > dd.govuk-summary-list__actions > a"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val alphaTwoCode: String
    val countryCodeValue: String
    val pensionPayments: String
    val pensionPaymentsValue: String
    val specialWithholdingTax: String
    val specialWithholdingTaxValue: String
    val foreignTaxCredit: String
    val yes: String
    val no: String
    val taxableAmount: String
    val taxableAmountValue: String
    val change: String
    val buttonText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Pension scheme summary"
    val alphaTwoCode: String = "Country"
    val countryCodeValue: String = "France"
    val pensionPayments: String = "Pensions payments"
    val pensionPaymentsValue: String = "Amount: £1,999.99 Non-uk tax: £1,999.99"
    val specialWithholdingTax: String = "Special Withholding Tax"
    val specialWithholdingTaxValue: String = "£1,999.99"
    val foreignTaxCredit: String = "Foreign Tax Credit"
    val yes: String = "Yes"
    val no: String = "No"
    val taxableAmount: String = "Taxable amount"
    val taxableAmountValue: String = "£1,999.99"
    val change: String = "Change"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Pension scheme summary"
    val alphaTwoCode: String = "Country"
    val countryCodeValue: String = "France"
    val pensionPayments: String = "Pensions payments"
    val pensionPaymentsValue: String = "Amount: £1,999.99 Non-uk tax: £1,999.99"
    val specialWithholdingTax: String = "Special Withholding Tax"
    val specialWithholdingTaxValue: String = "£1,999.99"
    val foreignTaxCredit: String = "Foreign Tax Credit"
    val yes: String = "Yes"
    val no: String = "No"
    val taxableAmount: String = "Taxable amount"
    val taxableAmountValue: String = "£1,999.99"
    val change: String = "Change"
    val buttonText = "Continue"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY))

  private lazy val underTest = inject[PensionsSchemeSummary]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the overseas summary page with full maxdata" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val overseasIncomePensionSchemes = IncomeFromOverseasPensionsViewModel(Some(true), Seq(
          PensionScheme(
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(1999.99),
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = Some(1999.99)
          )))
        val htmlFormat = underTest(OverseasPensionSchemeSummaryPage(taxYearEOY, aPensionsUserData.copy(pensions = aPensionsUserData.pensions.copy(incomeFromOverseasPensions = overseasIncomePensionSchemes)), None))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)

        textOnPageCheck(userScenario.commonExpectedResults.alphaTwoCode, selectors.getRowKey(1))
        textOnPageCheck(userScenario.commonExpectedResults.countryCodeValue, selectors.getRowValue(1))
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(1), controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeCountryController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.pensionPayments, selectors.getRowKey(2))
        textOnPageCheck(userScenario.commonExpectedResults.pensionPaymentsValue, selectors.getRowValue(2))
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(2), routes.PensionPaymentsController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.specialWithholdingTax, selectors.getRowKey(3))
        textOnPageCheck(userScenario.commonExpectedResults.specialWithholdingTaxValue, selectors.getRowValue(3), "for swt value")
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(3), routes.SpecialWithholdingTaxController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.foreignTaxCredit, selectors.getRowKey(4))
        textOnPageCheck(userScenario.commonExpectedResults.yes, selectors.getRowValue(4))
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(4), routes.ForeignTaxCreditReliefController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.taxableAmount, selectors.getRowKey(5))
        textOnPageCheck(userScenario.commonExpectedResults.taxableAmountValue, selectors.getRowValue(5))

        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the overseas summary page with minimum data" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val minPensionScheme = IncomeFromOverseasPensionsViewModel(Some(true), Seq(PensionScheme(
          alphaTwoCode = Some("FR"),
          pensionPaymentAmount = Some(1999.99),
          pensionPaymentTaxPaid = Some(1999.99),
          specialWithholdingTaxQuestion = Some(false),
          specialWithholdingTaxAmount = None,
          foreignTaxCreditReliefQuestion = Some(false),
          taxableAmount = None
        )))
        val htmlFormat = underTest(OverseasPensionSchemeSummaryPage(taxYearEOY, aPensionsUserData.copy(pensions = aPensionsUserData.pensions.copy(incomeFromOverseasPensions = minPensionScheme)), None))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)

        textOnPageCheck(userScenario.commonExpectedResults.alphaTwoCode, selectors.getRowKey(1))
        textOnPageCheck(userScenario.commonExpectedResults.countryCodeValue, selectors.getRowValue(1))
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(1), controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeCountryController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.pensionPayments, selectors.getRowKey(2))
        textOnPageCheck(userScenario.commonExpectedResults.pensionPaymentsValue, selectors.getRowValue(2))
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(2), routes.PensionPaymentsController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.specialWithholdingTax, selectors.getRowKey(3))
        textOnPageCheck(userScenario.commonExpectedResults.no, selectors.getRowValue(3), "for special withholding tax")
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(3), routes.SpecialWithholdingTaxController.show(taxYearEOY, None).url)

        textOnPageCheck(userScenario.commonExpectedResults.foreignTaxCredit, selectors.getRowKey(4))
        textOnPageCheck(userScenario.commonExpectedResults.no, selectors.getRowValue(4), "for foreign tax credits")
        linkCheck(userScenario.commonExpectedResults.change, selectors.getRowChange(4), routes.ForeignTaxCreditReliefController.show(taxYearEOY, None).url)

        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)

      }
    }
  }
}
// scalastyle:on magic.number
