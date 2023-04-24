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

import controllers.pensions.paymentsIntoOverseasPensions.routes
import models.pension.charges.{Relief, TaxReliefQuestion}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper
import models.pension.statebenefits.UkPensionIncomeViewModel

import scala.Option.option2Iterable

object PensionSchemeSummaryHelper extends CYABaseHelper {


  def summaryListRows(pensionIncomes :UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      pensionSchemeDetails(pensionIncomes, taxYear, index),
      pensionIncome(pensionIncomes, taxYear, index),
      pensionStartDate(pensionIncomes, taxYear, index),
    ).flatten

  def pensionSchemeDetails(pensionIncomes : UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {
    val provider = pensionIncomes.pensionSchemeName.getOrElse("")
    val paye = s"${messages("incomeFromPensions.schemeDetails.summary.paye")} ${pensionIncomes.pensionSchemeRef.getOrElse("")}"
    val pid = s"${messages("incomeFromPensions.schemeDetails.summary.pid")} ${pensionIncomes.pensionId.getOrElse("")}"
    Some(summaryListRowWithString(
      "incomeFromPensions.schemeDetails.summary.details",
      Some(Seq(provider, paye, pid)),
      routes.PensionsCustomerReferenceNumberController.show(taxYear, index)
    ))
  }

  def pensionIncome(pensionIncomes : UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {
    val pay = s"${messages("incomeFromPensions.schemeDetails.summary.pay")} ${pensionIncomes.amount.getOrElse("")}"
    val tax = s"${messages("incomeFromPensions.schemeDetails.summary.tax")} ${pensionIncomes.taxPaid.getOrElse("")}"
    Some(summaryListRowWithString(
        "incomeFromPensions.schemeDetails.summary.income",
        Some(Seq(pay, tax)),
      routes.PensionsCustomerReferenceNumberController.show(taxYear, index)) //change
    )
    }


  def pensionStartDate(pensionIncomes : UkPensionIncomeViewModel, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {
    val startDate = s"${messages("incomeFromPensions.schemeDetails.summary.date")} ${pensionIncomes.startDate.getOrElse("")}"
    Some(summaryListRowWithString(
        "incomeFromPensions.schemeDetails.summary.date",
        Some(Seq(startDate)),
        routes.PensionsCustomerReferenceNumberController.show(taxYear, index))
    )
  }
}
