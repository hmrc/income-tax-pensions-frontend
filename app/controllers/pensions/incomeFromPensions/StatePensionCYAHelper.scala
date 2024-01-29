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

import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper
import utils.DateTimeUtil.dateToStringFormat

object StatePensionCYAHelper extends CYABaseHelper {
  def summaryListRows(incomeFromPensionsViewModel: IncomeFromPensionsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] = {
    val s1 = Seq(
      statePensionSR(incomeFromPensionsViewModel.statePension, taxYear),
      startDateSR(incomeFromPensionsViewModel.statePension, taxYear),
      lumpSumSR(incomeFromPensionsViewModel.statePensionLumpSum, taxYear),
      lumpSumTaxSR(incomeFromPensionsViewModel.statePensionLumpSum, taxYear),
      lumSumSDateSR(incomeFromPensionsViewModel.statePensionLumpSum, taxYear),
      addStatePensionToTaxCalcSR(incomeFromPensionsViewModel.statePensionLumpSum, taxYear)
    ).flatten
    s1
  }

  private def statePensionSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    Some(
      summaryListRowWithString(
        "statePension.cya.statePension.label",
        Some(displayedValueForOptionalAmount(viewModel.flatMap(_.amount), messages("common.no"))),
        routes.StatePensionController.show(taxYear)
      ))

  private def startDateSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    viewModel.flatMap(sb =>
      sb.amountPaidQuestion
        .filter(x => x)
        .map(_ =>
          summaryListRowWithString(
            "statePension.cya.startDate.label",
            sb.startDate.map(st => st.format(dateToStringFormat)),
            routes.StatePensionStartDateController.show(taxYear)
          )))

  private def lumpSumSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    Some(
      summaryListRowWithString(
        "statePension.cya.lumpSum.label",
        Some(displayedValueForOptionalAmount(viewModel.flatMap(_.amount), messages("common.no"))),
        routes.StatePensionLumpSumController.show(taxYear)
      ))

  private def lumpSumTaxSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    viewModel.flatMap(sb =>
      sb.amountPaidQuestion
        .filter(x => x)
        .map(_ =>
          summaryListRowWithString(
            "statePension.cya.lumpSumTax.label",
            Some(displayedValueForOptionalAmount(viewModel.flatMap(_.taxPaid), messages("common.no"))),
            routes.TaxPaidOnStatePensionLumpSumController.show(taxYear)
          )))

  private def lumSumSDateSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    viewModel.flatMap(sb =>
      sb.amountPaidQuestion
        .filter(x => x)
        .map(_ =>
          summaryListRowWithString(
            "statePension.cya.lumpSumDate.label",
            sb.startDate.map(st => st.format(dateToStringFormat)),
            routes.StatePensionLumpSumStartDateController.show(taxYear)
          )))

  private def addStatePensionToTaxCalcSR(viewModel: Option[StateBenefitViewModel], taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    viewModel.flatMap(sb =>
      sb.addToCalculation
        .filter(_ != None)
        .map(_ =>
          summaryListRowWithString(
            "statePension.cya.taxCalc.label",
            Some(displayedValue(viewModel.flatMap(_.addToCalculation))),
            routes.StatePensionAddToCalculationController.show(taxYear)
          )))
}
