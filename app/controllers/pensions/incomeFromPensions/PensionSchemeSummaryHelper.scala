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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper
import models.pension.statebenefits.UkPensionIncomeViewModel
import utils.DateTimeUtil.{dateToStringFormat, localDateTimeFormat}

import java.time.LocalDate

object PensionSchemeSummaryHelper extends CYABaseHelper {

  def summaryListRows(pensionIncomes: UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      pensionSchemeDetails(pensionIncomes, taxYear, index),
      pensionIncome(pensionIncomes, taxYear, index),
      pensionStartDate(pensionIncomes, taxYear, index)
    )

  private def pensionSchemeDetails(pensionIncomes: UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit
      messages: Messages): SummaryListRow = {
    val provider = s"${pensionIncomes.pensionSchemeName.getOrElse("")}<br>"
    val paye     = s"${messages("incomeFromPensions.schemeDetails.summary.paye")} ${pensionIncomes.pensionSchemeRef.getOrElse("")}<br>"
    val pid      = s"${messages("incomeFromPensions.schemeDetails.summary.pid")} ${pensionIncomes.pensionId.getOrElse("")}"
    summaryListRowWithString(
      "incomeFromPensions.schemeDetails.summary.details",
      Some(provider + paye + pid),
      routes.PensionSchemeDetailsController.show(taxYear, index)
    )
  }

  def pensionIncome(pensionIncomes: UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): SummaryListRow = {
    val pay = s"${messages("incomeFromPensions.schemeDetails.summary.pay")} ${pensionIncomes.amount.getOrElse("")}<br>"
    val tax = s"${messages("incomeFromPensions.schemeDetails.summary.tax")} ${pensionIncomes.taxPaid.getOrElse("")}"
    summaryListRowWithString(
      "incomeFromPensions.schemeDetails.summary.income",
      Some(pay + tax),
      routes.PensionAmountController.show(taxYear, index)
    )
  }

  def pensionStartDate(pensionIncomes: UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): SummaryListRow = {
    val startDate          = pensionIncomes.startDate
    val parsedDate: String = startDate.fold("")(st => LocalDate.parse(st, localDateTimeFormat).format(dateToStringFormat))
    summaryListRowWithString(
      "incomeFromPensions.schemeDetails.summary.date",
      Some(parsedDate),
      routes.PensionSchemeStartDateController.show(taxYear, index)
    )
  }
}
