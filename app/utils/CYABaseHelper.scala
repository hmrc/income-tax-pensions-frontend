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

package utils


import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

import java.text.NumberFormat
import java.util.Locale


trait CYABaseHelper {
  def summaryListRow(labelMessageKey: String, displayedValue: String, changeLink: Call)(implicit messages: Messages): SummaryListRow = {
    ViewUtils.summaryListRow(
      HtmlContent(messages(labelMessageKey)),
      HtmlContent(displayedValue),
      actions = Seq(
        (changeLink, messages("common.change"),
          Some(messages(labelMessageKey + ".hidden"))))
    )
  }

  def summaryListRowWithBooleanValue(
                                      labelMessageKey: String,
                                      valueOpt: Option[Boolean],
                                      changeLink: Call,
                                      suffixOpt: Option[String] = None)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(valueOpt, suffixOpt), changeLink)

  def summaryListRowWithOptionalAmountValue(labelMessageKey: String, value: Option[BigDecimal], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForOptionalAmount(value), changeLink)

  def summaryListRowWithAmountValue(labelMessageKey: String, value: BigDecimal, changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(value), changeLink)

  def summaryListRowWithString(labelMessageKey: String, valueOpt: Option[Seq[String]], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForOptionalStrings(valueOpt), changeLink)

  def summaryListRowWithStrings(labelMessageKey: String, valueOpt: Option[String], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, valueOpt.getOrElse(""), changeLink)

  def summaryListRowWithAmountAndTaxValue(
                                           labelMessageKey: String,
                                           amount: Option[BigDecimal],
                                           taxPaid: Option[BigDecimal],
                                           changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForAmountAndTax(amount, taxPaid), changeLink)

  def displayedValueForOptionalAmount(valueOpt: Option[BigDecimal]): String = valueOpt.map(displayedValue).getOrElse("")

  def displayedValue(value: BigDecimal): String =  formatNoZeros(value)

  def formatNoZeros(amount: BigDecimal): String = {
    NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
      .replaceAll("\\.00", "")
  }

  def displayedValue(valueOpt: Option[Boolean], suffix: Option[String] = None)(implicit messages: Messages): String =
    valueOpt.map(value => if (value) {
      messages("common.yes")
    }
    else if (suffix.isDefined) {
      s"${messages("common.no")} ${messages(suffix.getOrElse(""))}"
    }
    else {
      messages("common.no")
    }).getOrElse("")

  def displayedValueForAmountAndTax(amount: Option[BigDecimal], taxPaid: Option[BigDecimal]): String =
    s"""Amount: ${displayedValueForOptionalAmount(amount)} <br> Tax paid: ${displayedValueForOptionalAmount(taxPaid)}"""


  def displayedValueForOptionalStrings(valueOpt: Option[Seq[String]]): String = valueOpt.map(_.mkString(", ")).getOrElse("")


  def removeSummaryListRowWithAmountValue(labelMessageKey: String, value: BigDecimal)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(value))

  def removeSummaryListRowWithString(labelMessageKey: String, valueOpt: Option[Seq[String]])(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForOptionalStrings(valueOpt))

  def summaryListRow(labelMessageKey: String, displayedValue: String)(implicit messages: Messages): SummaryListRow = {
    ViewUtils.removeSummaryListRow(
      HtmlContent(messages(labelMessageKey)),
      HtmlContent(displayedValue))
  }
}