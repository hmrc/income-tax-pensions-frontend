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

package controllers.pensions.transferIntoOverseasPensions

import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}
import org.scalatest.Assertion
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryListRow, Value}

class TransferIntoOverseasPensionCYAViewHelperSpec extends AnyWordSpec with Matchers { //scalastyle:off magic.number

  val taxYear = 2022

  implicit val messages: Messages = stubbedMessages()

  "Getting the summary rows" should {
    "return the expected" when {
      "we haven't provided any answers" in {

        val model = TransfersIntoOverseasPensionsViewModel()

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "")

      }

      "we have only partially completed the section (only the initial question)" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true))

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 2
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "No charge")
      }
      "we have answered no to the  overseasTransferCharge question " in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(false)
        )

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 2
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "No charge")
      }
      "we have answered yes and an amount to the overseasTransferCharge question" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(1000)
        )

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 3
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
      }
      "we have answered no to the pensionSchemeTransferCharge question" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(1000),
          pensionSchemeTransferCharge = Some(false)
        )

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 3
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
      }
      "we have answered yes to the pensionSchemeTransferCharge question" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(1000),
          pensionSchemeTransferCharge = Some(false),
          pensionSchemeTransferChargeAmount = Some(1000),
        )

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 3
        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
        assertRowTaxOnAmountCharged(summaryListRows(2), "No tax paid")
      }
      "our data for the 'surcharged' section has got into an unrealistic state, somehow" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(false),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(1000),
          pensionSchemeTransferCharge = Some(true),
          pensionSchemeTransferChargeAmount = Some(1000),
          transferPensionScheme = Seq(TransferPensionScheme(
            ukTransferCharge = Some(true),
            name = Some("Random Name"),
            pstr = Some("12345678RA"),
            qops = None,
            providerAddress = Some("Random Address"),
            alphaTwoCountryCode = None)))

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 1

        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "No")
      }
      "we have completed all sections" in {

        val model = TransfersIntoOverseasPensionsViewModel(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(1000),
          pensionSchemeTransferCharge = Some(true),
          pensionSchemeTransferChargeAmount = Some(1000),
          transferPensionScheme = Seq(TransferPensionScheme(
            ukTransferCharge = Some(true),
            name = Some("Random Name"),
            pstr = Some("12345678RA"),
            qops = None,
            providerAddress = Some("Random Address"),
            alphaTwoCountryCode = None)))

        val summaryListRows = TransferIntoOverseasPensionCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 4

        assertRowForTransfersIntoOverseasPensions(summaryListRows.head, "Yes")
        assertRowAmountCharged(summaryListRows(1), "£1,000")
        assertRowTaxOnAmountCharged(summaryListRows(2), "£1,000")
        assertRowForSchemesPayingTax(summaryListRows(3), "Random Name")
      }
    }
  }


  private def assertRowForTransfersIntoOverseasPensions(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "transferIntoOverseasPensions.cya.transferIntoOverseasPensions",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/overseas-transfer-charges/transfer-pension-savings",
      "transferIntoOverseasPensions.cya.transferIntoOverseasPensions.hidden"))
  }

  private def assertRowAmountCharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "transferIntoOverseasPensions.cya.amountCharged",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/overseas-transfer-charges/transfer-charge",
      "transferIntoOverseasPensions.cya.amountCharged.hidden"))
  }

  private def assertRowTaxOnAmountCharged(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "transferIntoOverseasPensions.cya.taxOnAmountCharged",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-tax",
      "transferIntoOverseasPensions.cya.taxOnAmountCharged.hidden"))
  }


  private def assertRowForSchemesPayingTax(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "transferIntoOverseasPensions.cya.schemesPayingTax",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/overseas-transfer-charges/transfer-charges-summary",
      "transferIntoOverseasPensions.cya.schemesPayingTax.hidden"))
  }


  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(summaryListRow, expectedSummaryRowContents.linkLabel, expectedSummaryRowContents.linkPathEnding, expectedSummaryRowContents.hiddenText)
  }

  private def assertAction(summaryListRow: SummaryListRow, expectedLabel: String, expectedPath: String, expectedHiddenText: String): Unit = {

    summaryListRow.actions shouldBe defined
    val actionsForFirstSummaryRow = summaryListRow.actions.get
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
      summaryListRow.key.content shouldBe HtmlContent(expectedLabel)
    }
  }

  private def assertValue(summaryListRow: SummaryListRow, expectedValue: String): Assertion = {
    withClue(s"We had expected the value to be '$expectedValue':") {
      summaryListRow.value shouldBe Value(HtmlContent(expectedValue), "govuk-!-width-one-third")
    }
  }

  private def stubbedMessages() = {
    import scala.jdk.CollectionConverters._

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
            "unauthorisedPayments.cya.pensionSchemeTaxReferences" -> "Pension Scheme Tax References",
            "common.noTaxPaid" -> "No tax paid",
            "transferIntoOverseasPensions.cya.noAmountCharged" -> "No charge"
          )
            .asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)

}
