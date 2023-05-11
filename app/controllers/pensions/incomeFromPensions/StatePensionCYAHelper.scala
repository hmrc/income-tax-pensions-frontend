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

import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper
import utils.DateTimeUtil.dateToStringFormat

object StatePensionCYAHelper extends CYABaseHelper {
  def summaryListRows(statePension :IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      statePensionSR(statePension, taxYear),
      startDateSR(statePension, taxYear),
      lumpSumSR(statePension, taxYear),
      lumpSumTaxSR(statePension, taxYear),
      lumSumSDateSR(statePension, taxYear),
      addStatePensionToTaxCalcsSR(statePension, taxYear)
    ).flatten

  private def statePensionSR(viewModel : IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(summaryListRowWithString(
      "statePension.cya.statePension.label",
      Some(displayedValueForOptionalAmount(viewModel.statePension.flatMap(_.amount), messages("common.no"))),
      routes.StatePensionController.show(taxYear)
    ))
  }

  private def startDateSR(viewModel : IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    viewModel.statePension.flatMap(sb => sb.amountPaidQuestion.filter(x =>x).map( _ =>
      summaryListRowWithString(
        "statePension.cya.startDate.label",
        sb.startDate.map(st => st.format(dateToStringFormat)),
        routes.StatePensionStartDateController.show(taxYear)
      )
    ))
  }

  private def lumpSumSR(viewModel : IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
   viewModel.statePensionLumpSum.flatMap(sb => sb.amountPaidQuestion.filter(x =>x).map( _ =>
     summaryListRowWithString(
      "statePension.cya.lumpSum.label",
      sb.amount.map(displayedValue),
      routes.StatePensionLumpSumController.show(taxYear)
     )
   ))
  }

  private def lumpSumTaxSR(viewModel: IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    viewModel.statePensionLumpSum.flatMap(sb => sb.taxPaidQuestion.filter(x => x).map(_ =>
      summaryListRowWithString(
        "statePension.cya.lumpSumTax.label",
        sb.taxPaid.map(displayedValue),
        routes.TaxPaidOnStatePensionLumpSumController.show(taxYear)
      )
    ))
  }

  private def lumSumSDateSR(viewModel: IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    viewModel.statePensionLumpSum.flatMap(sb => sb.amountPaidQuestion.filter(x => x).map(_ =>
      summaryListRowWithString(
        "statePension.cya.lumpSumDate.label",
        sb.startDate.map(st => st.format(dateToStringFormat)),
        routes.StatePensionLumpSumStartDateController.show(taxYear)
      )
    ))
  }

  private def addStatePensionToTaxCalcsSR(viewModel : IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    viewModel.statePension.flatMap(sb => sb.amountPaidQuestion.filter(x => x).map(_ =>  //TODO: replace with correct question
      summaryListRowWithString(
        "statePension.cya.taxCalc.label",
        Some(messages("common.yes")),
        routes.StatePensionLumpSumStartDateController.show(taxYear)  //TODO: replace by AddStatePensionToTaxCalcs
      )
    ))
  }
}