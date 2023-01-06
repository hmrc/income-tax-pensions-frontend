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

package controllers.pensions.unauthorisedPayments

import models.pension.charges.UnauthorisedPaymentsViewModel
import org.scalatest.Assertion
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryListRow, Value}

class UnauthorisedPaymentsCYAViewHelperTest extends AnyWordSpec with Matchers {

  val taxYear = 2022

  implicit val messages: Messages = stubbedMessages()

  "Getting the summary rows" should {
    "return the expected" when {
      "we haven't provided any answers" in {

        val model = UnauthorisedPaymentsViewModel()

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForUnauthorisedPayments(summaryListRows.head, "")

      }
      "we have only completed the 'surcharged' section" in {

        val model = UnauthorisedPaymentsViewModel(
          surchargeQuestion = Some(true),
          surchargeAmount = Option(BigDecimal("1000.00")),
          surchargeTaxAmountQuestion = Option(true),
          surchargeTaxAmount = Option(BigDecimal("120.00")),
          ukPensionSchemesQuestion = Some(true),
          pensionSchemeTaxReference = Some(Seq("12345678RX", "12345678RY"))
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 5
        assertRowForUnauthorisedPayments(summaryListRows.head, "Yes")
        assertRowAmountSurcharged(summaryListRows(1), "£1000.00")
        assertRowTaxOnAmountSurcharged(summaryListRows(2), "£120.00")
        assertRowForUKPensionSchemes(summaryListRows(3), "Yes")
        assertRowForUKPensionSchemeTaxReferences(summaryListRows(4), "12345678RX, 12345678RY")

      }
      "we have only partially completed the 'surcharged' section (only the initial question)" in {

        val model = UnauthorisedPaymentsViewModel(
          surchargeQuestion = Some(true),
          surchargeAmount = None,
          surchargeTaxAmountQuestion = None,
          surchargeTaxAmount = None,
          ukPensionSchemesQuestion = None,
          pensionSchemeTaxReference = None
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 4
        assertRowForUnauthorisedPayments(summaryListRows.head, "Yes")
        assertRowAmountSurcharged(summaryListRows(1), "")
        assertRowTaxOnAmountSurcharged(summaryListRows(2), "")
        assertRowForUKPensionSchemes(summaryListRows(3), "")

      }
      "our data for the 'surcharged' section has got into an unrealistic state, somehow" in {

        val model = UnauthorisedPaymentsViewModel(
          surchargeQuestion = Some(false),
          surchargeAmount = Option(BigDecimal("1000.00")),
          surchargeTaxAmountQuestion = Option(true),
          surchargeTaxAmount = Option(BigDecimal("120.00")),
          ukPensionSchemesQuestion = Some(true),
          pensionSchemeTaxReference = Some(Seq("12345678RX", "12345678RY"))
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForUnauthorisedPayments(summaryListRows.head, "No")

      }
      "we have only completed the 'not surcharged' section" in {

        val model = UnauthorisedPaymentsViewModel(
          noSurchargeQuestion = Some(true),
          noSurchargeAmount = Option(BigDecimal("800.00")),
          noSurchargeTaxAmountQuestion = Option(true),
          noSurchargeTaxAmount = Option(BigDecimal("8.80")),
          ukPensionSchemesQuestion = Some(true),
          pensionSchemeTaxReference = Some(Seq("12345678RX", "12345678RY"))
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 5
        assertRowForUnauthorisedPayments(summaryListRows.head, "Yes")
        assertRowAmountNotSurcharged(summaryListRows(1), "£800.00")
        assertRowTaxOnAmountNotSurcharged(summaryListRows(2), "£8.80")
        assertRowForUKPensionSchemes(summaryListRows(3), "Yes")
        assertRowForUKPensionSchemeTaxReferences(summaryListRows(4), "12345678RX, 12345678RY")

      }
      "we have only partially completed the 'not surcharged' section (only the initial question)" in {

        val model = UnauthorisedPaymentsViewModel(
          noSurchargeQuestion = Some(true),
          noSurchargeAmount = None,
          noSurchargeTaxAmountQuestion = None,
          noSurchargeTaxAmount = None,
          ukPensionSchemesQuestion = None,
          pensionSchemeTaxReference = None
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 4
        assertRowForUnauthorisedPayments(summaryListRows.head, "Yes")
        assertRowAmountNotSurcharged(summaryListRows(1), "")
        assertRowTaxOnAmountNotSurcharged(summaryListRows(2), "")
        assertRowForUKPensionSchemes(summaryListRows(3), "")


      }
      "our data for the 'not surcharged' section has got into an unrealistic state, somehow" in {

        val model = UnauthorisedPaymentsViewModel(
          noSurchargeQuestion = Some(false),
          noSurchargeAmount = Option(BigDecimal("800.00")),
          noSurchargeTaxAmountQuestion = Option(true),
          noSurchargeTaxAmount = Option(BigDecimal("8.80")),
          ukPensionSchemesQuestion = Some(true),
          pensionSchemeTaxReference = Some(Seq("12345678RX", "12345678RY"))
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForUnauthorisedPayments(summaryListRows.head, "No")

      }
      "we have declared that we have no unauthorised payments at all, with or without surcharge" in {

        val model = UnauthorisedPaymentsViewModel(
          noSurchargeQuestion = Some(false),
          surchargeQuestion = Some(false)
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForUnauthorisedPayments(summaryListRows.head, "No")

      }
      "we have completed all sections" in {

        val model = UnauthorisedPaymentsViewModel(
          surchargeQuestion = Some(true),
          surchargeAmount = Option(BigDecimal("2000.00")),
          surchargeTaxAmountQuestion = Option(true),
          surchargeTaxAmount = Option(BigDecimal("240.00")),
          noSurchargeQuestion = Some(true),
          noSurchargeAmount = Option(BigDecimal("1600.00")),
          noSurchargeTaxAmountQuestion = Option(true),
          noSurchargeTaxAmount = Option(BigDecimal("17.60")),
          ukPensionSchemesQuestion = Some(true),
          pensionSchemeTaxReference = Some(Seq("12345678RX", "12345678RY"))
        )

        val summaryListRows = UnauthorisedPaymentsCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 7

        assertRowForUnauthorisedPayments(summaryListRows.head, "Yes")
        assertRowAmountSurcharged(summaryListRows(1), "£2000.00")
        assertRowTaxOnAmountSurcharged(summaryListRows(2), "£240.00")
        assertRowAmountNotSurcharged(summaryListRows(3), "£1600.00")
        assertRowTaxOnAmountNotSurcharged(summaryListRows(4), "£17.60")
        assertRowForUKPensionSchemes(summaryListRows(5), "Yes")
        assertRowForUKPensionSchemeTaxReferences(summaryListRows(6), "12345678RX, 12345678RY")

      }
    }
  }

  private def assertRowForUnauthorisedPayments(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Unauthorised payments",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/unauthorised-payments",
      "unauthorisedPayments.common.title.hidden"))
  }

  private def assertRowAmountSurcharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Amount surcharged",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/amount-surcharged",
      "unauthorisedPayments.cya.amountSurcharged.hidden"))
  }

  private def assertRowTaxOnAmountSurcharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Non UK-tax on amount surcharged",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/tax-on-amount-surcharged",
      "unauthorisedPayments.cya.nonUkTaxAmountSurcharged.hidden"))
  }

  private def assertRowAmountNotSurcharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Amount not surcharged",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/amount-not-surcharged",
      "unauthorisedPayments.cya.amountNotSurcharged.hidden"))
  }

  private def assertRowTaxOnAmountNotSurcharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Non UK-tax on amount not surcharged",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/tax-on-amount-not-surcharged",
      "unauthorisedPayments.cya.nonUkTaxAmountNotSurcharged.hidden"))
  }

  private def assertRowForUKPensionSchemes(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "UK pension schemes",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/uk-pension-scheme",
      "unauthorisedPayments.common.ukPensionSchemes.hidden"))
  }

  private def assertRowForUKPensionSchemeTaxReferences(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Pension Scheme Tax References",
      expectedValue,
      "Change",
      "/2022/unauthorised-payments-from-pensions/pension-scheme-tax-reference",
      "unauthorisedPayments.cya.pensionSchemeTaxReferences.hidden"))
  }

  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(summaryListRow, expectedSummaryRowContents.linkLabel, expectedSummaryRowContents.linkPathEnding, expectedSummaryRowContents.hiddenText)
  }

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
            "unauthorisedPayments.common.title" -> "Unauthorised payments",
            "common.yes" -> "Yes",
            "common.no" -> "No",
            "common.change" -> "Change",
            "unauthorisedPayments.cya.amountSurcharged" -> "Amount surcharged",
            "unauthorisedPayments.cya.nonUkTaxAmountSurcharged" -> "Non UK-tax on amount surcharged",
            "unauthorisedPayments.cya.amountNotSurcharged" -> "Amount not surcharged",
            "unauthorisedPayments.cya.nonUkTaxAmountNotSurcharged" -> "Non UK-tax on amount not surcharged",
            "unauthorisedPayments.common.ukPensionSchemes" -> "UK pension schemes",
            "unauthorisedPayments.cya.pensionSchemeTaxReferences" -> "Pension Scheme Tax References"
          )
            .asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)

}
