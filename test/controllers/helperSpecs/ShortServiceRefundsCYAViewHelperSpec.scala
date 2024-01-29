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

package controllers.helperSpecs

import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, minimalShortServiceRefundsViewModel}
import controllers.pensions.shortServiceRefunds.ShortSummaryCYAViewHelper
import org.scalatest.Assertion
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryListRow, Value}

class ShortServiceRefundsCYAViewHelperSpec extends AnyWordSpec with Matchers {

  val taxYear = 2022

  implicit val messages: Messages = stubbedMessages()

  "Getting the summary rows" should {
    "return the expected" when {
      "we have only answered 'no' to the initial question" in {
        val summaryListRows = ShortSummaryCYAViewHelper.summaryListRows(minimalShortServiceRefundsViewModel, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForRefundBoolean(summaryListRows.head, "No")
      }

      "we answer 'no' to non-UK tax on short service refunds" in {
        val model = aShortServiceRefundsViewModel.copy(
          shortServiceRefundTaxPaid = Some(false),
          shortServiceRefundTaxPaidCharge = None
        )
        val summaryListRows = ShortSummaryCYAViewHelper.summaryListRows(model, taxYear)

        summaryListRows.length shouldBe 4
        assertRowForRefundBoolean(summaryListRows.head, "Yes")
        assertRowForRefundAmount(summaryListRows(1), "£1,999.99")
        assertRowForNonUKBoolean(summaryListRows(2), "No tax paid")
        assertRowForSchemes(summaryListRows(3), "Scheme Name with UK charge, Scheme Name without UK charge")
      }

      "we have completed all sections" in {
        val summaryListRows = ShortSummaryCYAViewHelper.summaryListRows(aShortServiceRefundsViewModel, taxYear)

        summaryListRows.length shouldBe 5
        assertRowForRefundBoolean(summaryListRows.head, "Yes")
        assertRowForRefundAmount(summaryListRows(1), "£1,999.99")
        assertRowForNonUKBoolean(summaryListRows(2), "Yes")
        assertRowForNonUKAmount(summaryListRows(3), "£1,000")
        assertRowForSchemes(summaryListRows(4), "Scheme Name with UK charge, Scheme Name without UK charge")
      }
    }
  }

  private def assertRowForRefundBoolean(summaryListRow: SummaryListRow, expectedValue: String): Unit =
    assertSummaryListRow(
      summaryListRow,
      ExpectedSummaryRowContents(
        "Short service refunds",
        expectedValue,
        "Change",
        "/2022/overseas-pensions/short-service-refunds/taxable-short-service-refunds",
        "Change short service refunds"
      )
    )

  private def assertRowForRefundAmount(summaryListRow: SummaryListRow, expectedValue: String): Unit =
    assertSummaryListRow(
      summaryListRow,
      ExpectedSummaryRowContents(
        "Refund amount",
        expectedValue,
        "Change",
        "/2022/overseas-pensions/short-service-refunds/taxable-short-service-refunds",
        "Change refund amount")
    )

  private def assertRowForNonUKBoolean(summaryListRow: SummaryListRow, expectedValue: String): Unit =
    assertSummaryListRow(
      summaryListRow,
      ExpectedSummaryRowContents(
        "Paid non-UK tax",
        expectedValue,
        "Change",
        "/2022/overseas-pensions/short-service-refunds/short-service-refund-non-uk-tax",
        "Change paid non-UK tax")
    )

  private def assertRowForNonUKAmount(summaryListRow: SummaryListRow, expectedValue: String): Unit =
    assertSummaryListRow(
      summaryListRow,
      ExpectedSummaryRowContents(
        "Amount of non-UK tax",
        expectedValue,
        "Change",
        "/2022/overseas-pensions/short-service-refunds/short-service-refund-non-uk-tax",
        "Change amount of non-UK tax"
      )
    )

  private def assertRowForSchemes(summaryListRow: SummaryListRow, expectedValue: String): Unit =
    assertSummaryListRow(
      summaryListRow,
      ExpectedSummaryRowContents(
        "Pension schemes paying tax",
        expectedValue,
        "Change",
        "/2022/overseas-pensions/short-service-refunds/short-service-refund-summary",
        "Change pension schemes paying tax"
      )
    )

  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(
      summaryListRow,
      expectedSummaryRowContents.linkLabel,
      expectedSummaryRowContents.linkPathEnding,
      expectedSummaryRowContents.hiddenText)
  }

  private def assertAction(summaryListRow: SummaryListRow, expectedLabel: String, expectedPath: String, expectedHiddenText: String): Unit = {

    summaryListRow.actions shouldBe defined
    val actionsForFirstSummaryRow = summaryListRow.actions.get
    val firstAction: ActionItem   = actionsForFirstSummaryRow.items.head
    firstAction.content shouldBe HtmlContent("<span aria-hidden=\"true\">" + expectedLabel + "</span>")
    withClue(s"We had expected the link path to end with '$expectedPath':") {
      firstAction.href should endWith(expectedPath)
    }
    firstAction.visuallyHiddenText shouldBe defined
    firstAction.visuallyHiddenText.get shouldBe expectedHiddenText
  }

  private def assertLabel(summaryListRow: SummaryListRow, expectedLabel: String) =
    withClue(s"We had expected the label to be '$expectedLabel':") {
      summaryListRow.key.content shouldBe HtmlContent(expectedLabel)
    }

  private def assertValue(summaryListRow: SummaryListRow, expectedValue: String): Assertion =
    withClue(s"We had expected the value to be '$expectedValue':") {
      summaryListRow.value shouldBe Value(HtmlContent(expectedValue), "govuk-!-width-one-third")
    }

  private def stubbedMessages() = {
    import scala.jdk.CollectionConverters._

    val messagesApi = stubMessagesApi(
      Map(
        Lang.defaultLang.code ->
          Map(
            "unauthorisedPayments.common.title"               -> "Unauthorised payments",
            "common.yes"                                      -> "Yes",
            "common.no"                                       -> "No",
            "common.change"                                   -> "Change",
            "common.noTaxPaid"                                -> "No tax paid",
            "shortServiceRefunds.cya.refund"                  -> "Short service refunds",
            "shortServiceRefunds.cya.refundAmount"            -> "Refund amount",
            "shortServiceRefunds.cya.nonUk"                   -> "Paid non-UK tax",
            "shortServiceRefunds.cya.nonUkAmount"             -> "Amount of non-UK tax",
            "shortServiceRefunds.cya.schemesPayingTax"        -> "Pension schemes paying tax",
            "shortServiceRefunds.cya.refund.hidden"           -> "Change short service refunds",
            "shortServiceRefunds.cya.refundAmount.hidden"     -> "Change refund amount",
            "shortServiceRefunds.cya.nonUk.hidden"            -> "Change paid non-UK tax",
            "shortServiceRefunds.cya.nonUkAmount.hidden"      -> "Change amount of non-UK tax",
            "shortServiceRefunds.cya.schemesPayingTax.hidden" -> "Change pension schemes paying tax"
          ).asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs())
    )
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)

}
