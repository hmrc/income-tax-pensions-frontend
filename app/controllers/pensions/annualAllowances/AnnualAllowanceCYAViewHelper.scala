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

package controllers.pensions.annualAllowances

import controllers.pensions.annualAllowances.{routes => annualRoutes}
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object AnnualAllowanceCYAViewHelper extends CYABaseHelper {

  def summaryListRows(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForReducedAnnualAllowanceQuestion(annualAllowancesViewModel, taxYear),
      summaryRowTypeOfReducedAnnualAllowanceQuestion(annualAllowancesViewModel, taxYear),
      summaryRowForAboveAnnualAllowance(annualAllowancesViewModel, taxYear),
      summaryRowForAmountAboveAnnualAllowance(annualAllowancesViewModel, taxYear),
      summaryRowForAnnualAllowanceTax(annualAllowancesViewModel, taxYear),
      summaryRowForAnnualAllowanceSchemeTaxReferences(annualAllowancesViewModel, taxYear)
    ).flatten

  private def summaryRowForReducedAnnualAllowanceQuestion(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .map(_ =>
        summaryListRowWithBooleanValue(
          "lifetimeAllowance.cya.reducedAnnualAllowance",
          annualAllowancesViewModel.reducedAnnualAllowanceQuestion,
          annualRoutes.ReducedAnnualAllowanceController.show(taxYear)
        )(messages))

  private def summaryRowTypeOfReducedAnnualAllowanceQuestion(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .filter(x => x)
      .map { _ =>
        summaryListRowWithStrings(
          "lifetimeAllowance.cya.typeOfReducedAnnualAllowance",
          annualAllowancesViewModel.typeOfAllowance,
          annualRoutes.ReducedAnnualAllowanceTypeController.show(taxYear)
        )(messages)
      }

  private def summaryRowForAboveAnnualAllowance(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .filter(x => x)
      .map(_ =>
        summaryListRowWithBooleanValue(
          "lifetimeAllowance.cya.aboveAnnualAllowance",
          annualAllowancesViewModel.aboveAnnualAllowanceQuestion,
          annualRoutes.AboveReducedAnnualAllowanceController.show(taxYear)
        )(messages))

  private def summaryRowForAmountAboveAnnualAllowance(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .filter(x => x)
      .flatMap(_ =>
        annualAllowancesViewModel.aboveAnnualAllowanceQuestion
          .filter(x => x)
          .map(_ =>
            summaryListRowWithOptionalAmountValue(
              "annualAllowance.cya.amountAboveAnnualAllowance",
              annualAllowancesViewModel.aboveAnnualAllowance,
              annualRoutes.AboveReducedAnnualAllowanceController.show(taxYear)
            )(messages)))

  private def summaryRowForAnnualAllowanceTax(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .filter(x => x)
      .flatMap(_ =>
        annualAllowancesViewModel.aboveAnnualAllowanceQuestion
          .filter(x => x)
          .map(_ =>
            annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion match {
              case Some(true) if annualAllowancesViewModel.taxPaidByPensionProvider.isDefined =>
                summaryListRowWithOptionalAmountValue(
                  "lifetimeAllowance.cya.annualAllowanceTax",
                  annualAllowancesViewModel.taxPaidByPensionProvider,
                  routes.PensionProviderPaidTaxController.show(taxYear))(messages)
              case _ =>
                summaryListRowWithBooleanValue(
                  "lifetimeAllowance.cya.annualAllowanceTax",
                  annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion,
                  routes.PensionProviderPaidTaxController.show(taxYear)
                )(messages)
            }))

  private def summaryRowForAnnualAllowanceSchemeTaxReferences(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit
      messages: Messages): Option[SummaryListRow] =
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion
      .filter(x => x)
      .flatMap(_ =>
        annualAllowancesViewModel.aboveAnnualAllowanceQuestion
          .filter(x => x)
          .flatMap(_ =>
            annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion
              .filter(x => x)
              .map(_ =>
                summaryListRowWithStrings(
                  "lifetimeAllowance.cya.annualPensionSchemeTaxReferences",
                  annualAllowancesViewModel.pensionSchemeTaxReferences,
                  annualRoutes.PstrSummaryController.show(taxYear)
                )(messages))))
}
