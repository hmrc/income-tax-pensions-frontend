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

package controllers.pensions.lifetimeAllowances

import models.pension.charges.PensionLifetimeAllowancesViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper


object LifetimeAllowanceCYAViewHelper extends CYABaseHelper {

  def summaryListRows(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                     (implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForAboveLifetimeAllowanceQuestion(lifetimeAllowancesViewModel, taxYear),
      summaryRowForLumpSumAmounts(lifetimeAllowancesViewModel, taxYear),
      summaryRowForOtherPayments(lifetimeAllowancesViewModel, taxYear),
      summaryRowForLifetimeAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel, taxYear)
    ).flatten


  private def summaryRowForAboveLifetimeAllowanceQuestion(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                                                         (implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "lifetimeAllowance.cya.aboveLifetimeAllowance",
        lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion,
        routes.AboveAnnualLifetimeAllowanceController.show(taxYear))(messages)
      )
  }

  private def summaryRowForLumpSumAmounts(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                                         (implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(x => x).map { _ =>
      lifetimeAllowancesViewModel.pensionAsLumpSumQuestion match {
        case Some(true) if lifetimeAllowancesViewModel.pensionAsLumpSumQuestion.isDefined =>
          summaryListRowWithAmountAndTaxValue(
            "lifetimeAllowance.cya.lumpSum",
            lifetimeAllowancesViewModel.pensionAsLumpSum.flatMap(_.amount),
            lifetimeAllowancesViewModel.pensionAsLumpSum.flatMap(_.taxPaid),
            routes.PensionLumpSumController.show(taxYear))(messages)
        case _ =>
          summaryListRowWithBooleanValue(
            "lifetimeAllowance.cya.lumpSum",
            lifetimeAllowancesViewModel.pensionAsLumpSumQuestion,
            routes.PensionLumpSumController.show(taxYear))(messages)
      }
    }
  }

  private def summaryRowForOtherPayments(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                                        (implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(x => x).map { _ =>
      lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion match {
        case Some(true) if lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion.isDefined =>
          summaryListRowWithAmountAndTaxValue(
            "lifetimeAllowance.cya.otherPayments",
            lifetimeAllowancesViewModel.pensionPaidAnotherWay.flatMap(_.amount),
            lifetimeAllowancesViewModel.pensionPaidAnotherWay.flatMap(_.taxPaid),
            routes.LifeTimeAllowanceAnotherWayController.show(taxYear))(messages)
        case _ =>
          summaryListRowWithBooleanValue(
            "lifetimeAllowance.cya.otherPayments",
            lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion,
            routes.LifeTimeAllowanceAnotherWayController.show(taxYear))(messages)
      }
    }
  }

  private def summaryRowForLifetimeAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                                                               (implicit messages: Messages): Option[SummaryListRow] = {

    val combinedLumpSumOtherPaymentsQuestion = (
      lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion, lifetimeAllowancesViewModel.pensionAsLumpSumQuestion) match {
      case (_, Some(true)) => Some(true)
      case (Some(true), _) => Some(true)
      case _ => None
    }

    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(x => x).flatMap(_ =>
      combinedLumpSumOtherPaymentsQuestion.filter(x => x).map { _ =>
        summaryListRowWithStrings(
          "lifetimeAllowance.cya.lifetimePensionSchemeTaxReferences",
          lifetimeAllowancesViewModel.pensionSchemeTaxReferences,
          controllers.pensions.lifetimeAllowances.routes.LifetimePstrSummaryController.show(taxYear))(messages)
      }
    )
  }
}
