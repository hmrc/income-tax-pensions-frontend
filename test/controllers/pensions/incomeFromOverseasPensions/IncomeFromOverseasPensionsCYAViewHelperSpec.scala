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

package controllers.pensions.incomeFromOverseasPensions

import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryListRow, Value}
import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import views.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsCYAViewHelper

class IncomeFromOverseasPensionsCYAViewHelperSpec extends AnyWordSpec with Matchers {

  val taxYear = 2022

  implicit val messages: Messages = stubbedMessages()

  "Getting the summary rows" should {
    "return the expected" when {
      "we haven't provided any answers" in {

        val model = IncomeFromOverseasPensionsViewModel()

        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "")

      }
      "we have income from overseas pension from one country" in {

        val model = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(PensionScheme(
            alphaThreeCode = Some("FRA"),
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(100),
            pensionPaymentTaxPaid = Some(100),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(100),
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = Some(100)
          ))
        )
        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 2
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "Yes")
        assertRowForOverseasPensionSchemes(summaryListRows(1), "FRANCE")
      }

      "we have incomes from overseas pension from three different countries" in {

        val model = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(PensionScheme(
            alphaThreeCode = Some("FRA"),
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(100),
            pensionPaymentTaxPaid = Some(100),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(100),
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = Some(100)
          ),
            PensionScheme(
              alphaThreeCode = Some("IND"),
              alphaTwoCode = Some("IN"),
              pensionPaymentAmount = Some(100),
              pensionPaymentTaxPaid = Some(100),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(100),
              foreignTaxCreditReliefQuestion = Some(true),
              taxableAmount = Some(100)
            ),
            PensionScheme(
              alphaThreeCode = Some("SLE"),
              alphaTwoCode = Some("SL"),
              pensionPaymentAmount = Some(100),
              pensionPaymentTaxPaid = Some(100),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(100),
              foreignTaxCreditReliefQuestion = Some(true),
              taxableAmount = Some(100)
            )
          )
        )

        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 2
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "Yes")
        assertRowForOverseasPensionSchemes(summaryListRows(1), "FRANCE, INDIA, SIERRA LEONE")
      }

      "we selected 'No' for paymentsFromOverseasPensionsQuestion and yet somehow passed in a pension income " in {

        val model = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(false),
          overseasIncomePensionSchemes = Seq(PensionScheme(
            alphaThreeCode = Some("FRA"),
            pensionPaymentAmount = Some(100),
            pensionPaymentTaxPaid = Some(100),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(100),
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = Some(100)
          ))
        )
        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "No")
      }

      "we selected 'No' for paymentsFromOverseasPensionsQuestion and there is no pension income " in {

        val model = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(false)
        )
        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "No")
      }

      "we received a wrong country code from backend service " in {

        val wrongOverseasIncomePensionSchemes = Seq(
          PensionScheme(
            alphaThreeCode = Some("ABC"),
            alphaTwoCode = None,
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(1999.99),
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1999.99)
          ),
          PensionScheme(
            alphaThreeCode = Some("XXX"),
            alphaTwoCode = None,
            pensionPaymentAmount = Some(2000.00),
            pensionPaymentTaxPaid = Some(400.00),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(400.00),
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1600.00)
          ))

        val updatedModel = anIncomeFromOverseasPensionsViewModel.copy(
          overseasIncomePensionSchemes = wrongOverseasIncomePensionSchemes
        )

        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(updatedModel, taxYear)

        summaryListRows.length shouldBe 2
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "Yes")
        assertRowForOverseasPensionSchemes(summaryListRows(1), "NO COUNTRY CODE, NO COUNTRY CODE")
      }

      "we selected 'Yes' for paymentsFromOverseasPensionsQuestion and yet somehow did not pass a pension income " in {

        val model = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq()
        )
        val summaryListRows = IncomeFromOverseasPensionsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForPaymentsFromOverseasPensions(summaryListRows.head, "Yes")
      }
    }
  }

  private def assertRowForPaymentsFromOverseasPensions(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Payments from overseas pensions",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-status",
      "incomeFromOverseasPensions.cya.paymentsFromOverseasPensions.hidden"))
  }

  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(summaryListRow, expectedSummaryRowContents.linkLabel, expectedSummaryRowContents.linkPathEnding, expectedSummaryRowContents.hiddenText)
  }

  private def assertRowForOverseasPensionSchemes(summaryListRow: SummaryListRow, expectedValue: String, index : Int = 0): Unit =
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Overseas pension schemes",
      expectedValue,
      "Change",
      s"/2022/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country-summary",
      "incomeFromOverseasPensions.cya.overseasPensionSchemes.hidden"))

  private def assertAction(summaryListRow: SummaryListRow, expectedLabel: String, expectedPath: String, expectedHiddenText: String): Unit = {

    summaryListRow.actions shouldBe defined
    val actionsForFirstSummaryRow = summaryListRow.actions.get
    actionsForFirstSummaryRow.items.length shouldBe 1
    val firstAction: ActionItem = actionsForFirstSummaryRow.items.head
    firstAction.content shouldBe HtmlContent("<span aria-hidden=\"true\">" + expectedLabel + "</span>")
    withClue(s"We had expected the link path to end with '$expectedPath':") {
      firstAction.href should endWith(expectedPath)
    }
    firstAction.visuallyHiddenText shouldBe defined
    firstAction.visuallyHiddenText.get shouldBe expectedHiddenText
  }

  private def assertLabel(summaryListRow: SummaryListRow, expectedLabel: String) = {
    withClue(s"We had expected the label to be '$expectedLabel':") {
      summaryListRow.key shouldBe Key(HtmlContent(expectedLabel), "govuk-!-width-one-third")
    }
  }

  private def assertValue(summaryListRow: SummaryListRow, expectedValue: String): Assertion = {
    withClue(s"We had expected the value to be '$expectedValue':") {
      summaryListRow.value shouldBe Value(HtmlContent(expectedValue), "govuk-!-width-one-third")
    }
  }

  private def stubbedMessages() = {
    import collection.JavaConverters._

    val messagesApi = stubMessagesApi(
      Map(
        Lang.defaultLang.code ->
          Map(
            "incomeFromOverseasPensions.cya.paymentsFromOverseasPensions" -> "Payments from overseas pensions",
            "common.yes" -> "Yes",
            "common.no" -> "No",
            "common.change" -> "Change",
            "incomeFromOverseasPensions.cya.overseasPensionSchemes" -> "Overseas pension schemes"
          )
            .asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)
}
