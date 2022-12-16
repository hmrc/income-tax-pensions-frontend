/*
 * Copyright 2022 HM Revenue & Customs
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


trait CYABaseHelper{
  def summaryListRow(labelMessageKey: String, displayedValue: String, changeLink: Call)(implicit messages: Messages): SummaryListRow = {
    ViewUtils.summaryListRow(
      HtmlContent(messages(labelMessageKey)),
      HtmlContent(displayedValue),
      actions = Seq(
        (changeLink, messages("common.change"),
          Some(messages(labelMessageKey + ".hidden"))))
    )
  }

  def summaryListRowWithBooleanValue(labelMessageKey: String, valueOpt: Option[Boolean], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(valueOpt), changeLink)

   def summaryListRowWithOptionalAmountValue(labelMessageKey: String, value: Option[BigDecimal], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForOptionalAmount(value), changeLink)

   def summaryListRowWithAmountValue(labelMessageKey: String, value: BigDecimal, changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(value), changeLink)

   def summaryListRowWithString(labelMessageKey: String, valueOpt: Option[Seq[String]], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForOptionalStrings(valueOpt), changeLink)

  def summaryListRowWithStrings(labelMessageKey: String, valueOpt: Option[String], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, valueOpt.getOrElse(""), changeLink)

   def summaryListRowWithAmountAndTaxValue(labelMessageKey: String, amount: Option[BigDecimal], taxPaid: Option[BigDecimal], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValueForAmountAndTax(amount, taxPaid), changeLink)

  def displayedValueForOptionalAmount(valueOpt: Option[BigDecimal]): String = valueOpt.map(displayedValue).getOrElse("")

   def displayedValue(value: BigDecimal): String = if (value == 0) "" else s"£$value"

   def displayedValue(valueOpt: Option[Boolean])(implicit messages: Messages): String =
    valueOpt.map(value => if (value) messages("common.yes") else messages("common.no")).getOrElse("")

   def displayedValueForAmountAndTax(amount: Option[BigDecimal], taxPaid: Option[BigDecimal]): String =
    s"""Amount: ${displayedValueForOptionalAmount(amount)} <br> Tax paid: ${displayedValueForOptionalAmount(taxPaid)}"""


  def displayedValueForOptionalStrings(valueOpt: Option[Seq[String]]): String = valueOpt.map(_.mkString(", ")).getOrElse("")
  
}