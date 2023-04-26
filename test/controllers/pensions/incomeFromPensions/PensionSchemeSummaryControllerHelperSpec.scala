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

package controllers.pensions.incomeFromPensions

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import models.pension.statebenefits.UkPensionIncomeViewModel
import org.scalatest.Assertion
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryListRow, Value}

class PensionSchemeSummaryControllerHelperSpec extends AnyWordSpec with Matchers{

  implicit val messages: Messages = stubbedMessages()

  val taxYear = 2022
  val index: Option[Int] = Some(0)

  val incomeModel: UkPensionIncomeViewModel = UkPensionIncomeViewModel(
    Some("123/AB456"), Some("1234"), Some("01 01 2008"), None, None, Some("pro"), Some(200), Some(80))

  val incomeModelNone: UkPensionIncomeViewModel = UkPensionIncomeViewModel(
    None, None, None, None, None, None)

  "Getting the summary rows" should {
    "return the expected" when {
      "we have provided all answers for income from pensions" in {
        val summaryListRows = PensionSchemeSummaryHelper.summaryListRows(incomeModel, taxYear, index)

        summaryListRows.length shouldBe 3
        assertRowForSchemeDetails(summaryListRows.head, "pro<br>PAYE: 123/AB456<br>PID: 1234")
        assertRowForPensionsIncome(summaryListRows(1), "Pay: 200<br>Tax: 80")
        assertRowForPensionStartDate(summaryListRows(2), "January 1 2008")
      }
      "we have provided none of the above" in {
        val summaryListRows = PensionSchemeSummaryHelper.summaryListRows(incomeModelNone, taxYear, index)

        summaryListRows.length shouldBe 3
        assertRowForSchemeDetails(summaryListRows.head, "<br>PAYE: <br>PID: ")
        assertRowForPensionsIncome(summaryListRows(1), "Pay: <br>Tax: ")
        assertRowForPensionStartDate(summaryListRows(2), "")
      }
    }
  }

  private def assertRowForSchemeDetails(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    val addOn = if (index.isDefined) s"?pensionSchemeIndex=${index.get}" else ""
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Scheme details",
      expectedValue,
      "common.change",
      s"/2022/pension-income/pension-income-details$addOn",
      messages("incomeFromPensions.schemeDetails.summary.details.hidden")))
  }

  private def assertRowForPensionsIncome(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    val addOn = if (index.isDefined) s"?pensionSchemeIndex=${index.get}" else ""
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Pension income",
      expectedValue,
      "common.change",
      s"/2022/pension-income/pension-amount$addOn",
      messages("incomeFromPensions.schemeDetails.summary.income.hidden")))
  }

  private def assertRowForPensionStartDate(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    val addOn = if (index.isDefined) s"?pensionSchemeIndex=${index.get}" else ""
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Pension start date",
      expectedValue,
      "common.change",
      s"/2022/pension-income/pension-start-date$addOn",
      messages("incomeFromPensions.schemeDetails.summary.date.hidden")))
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
            "incomeFromPensions.schemeDetails.summary.title" -> "Check pension scheme details",
            "incomeFromPensions.schemeDetails.summary.details" -> "Scheme details",
            "incomeFromPensions.schemeDetails.summary.income" -> "Pension income",
            "incomeFromPensions.schemeDetails.summary.date" -> "Pension start date",
            "incomeFromPensions.schemeDetails.summary.paye" -> "PAYE:",
            "incomeFromPensions.schemeDetails.summary.pid" -> "PID:",
            "incomeFromPensions.schemeDetails.summary.pay" -> "Pay:",
            "incomeFromPensions.schemeDetails.summary.tax" -> "Tax:"
          ).asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)
}
