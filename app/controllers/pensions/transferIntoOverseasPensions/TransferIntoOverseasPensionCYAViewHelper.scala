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

package controllers.pensions.transferIntoOverseasPensions

import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object TransferIntoOverseasPensionCYAViewHelper extends CYABaseHelper {


  def summaryListRows(transfersIntoOverseasPensionsViewModel: TransfersIntoOverseasPensionsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForTransferIntoOverseasPensions(transfersIntoOverseasPensionsViewModel, taxYear),
      summaryRowForAmountCharged(transfersIntoOverseasPensionsViewModel, taxYear),
      summaryRowForTaxOnAmountCharged(transfersIntoOverseasPensionsViewModel, taxYear),
      summaryRowForSchemesPayingTax(transfersIntoOverseasPensionsViewModel, taxYear)
    ).flatten

  private def summaryRowForTransferIntoOverseasPensions(transfersIntoOverseasPensionsViewModel: TransfersIntoOverseasPensionsViewModel, taxYear: Int)
                                                       (implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "transferIntoOverseasPensions.cya.transferIntoOverseasPensions",
        transfersIntoOverseasPensionsViewModel.transferPensionSavings,
        routes.TransferPensionSavingsController.show(taxYear))(messages)
    )
  }

  private def summaryRowForAmountCharged(transfersIntoOverseasPensionsViewModel: TransfersIntoOverseasPensionsViewModel, taxYear: Int)
                                        (implicit messages: Messages): Option[SummaryListRow] = {
    transfersIntoOverseasPensionsViewModel.transferPensionSavings
      .filter(_ == true)
      .map(_ =>
        transfersIntoOverseasPensionsViewModel.overseasTransferCharge match {
          case Some(true) if transfersIntoOverseasPensionsViewModel.overseasTransferChargeAmount.isDefined =>
            summaryListRowWithOptionalAmountValue(
              "transferIntoOverseasPensions.cya.amountCharged",
              transfersIntoOverseasPensionsViewModel.overseasTransferChargeAmount,
              routes.OverseasTransferChargeController.show(taxYear))(messages)
          case _ =>
            summaryListRowWithString(
              "transferIntoOverseasPensions.cya.amountCharged",
              Some(messages("transferIntoOverseasPensions.cya.noAmountCharged")).map(Seq(_)),
              routes.OverseasTransferChargeController.show(taxYear)
            )(messages)
        }
      )
  }

  private def summaryRowForTaxOnAmountCharged(transfersIntoOverseasPensionsViewModel: TransfersIntoOverseasPensionsViewModel, taxYear: Int)
                                             (implicit messages: Messages): Option[SummaryListRow] = {
    transfersIntoOverseasPensionsViewModel.transferPensionSavings.filter(_ == true).flatMap { _ =>
      transfersIntoOverseasPensionsViewModel.overseasTransferCharge.filter(_ == true).map(_ =>
        transfersIntoOverseasPensionsViewModel.pensionSchemeTransferCharge match {
          case Some(true) if transfersIntoOverseasPensionsViewModel.pensionSchemeTransferChargeAmount.isDefined =>
            summaryListRowWithOptionalAmountValue(
              "transferIntoOverseasPensions.cya.taxOnAmountCharged",
              transfersIntoOverseasPensionsViewModel.pensionSchemeTransferChargeAmount,
              routes.PensionSchemeTaxTransferController.show(taxYear))(messages)
          case _ =>
            summaryListRowWithString(
              "transferIntoOverseasPensions.cya.taxOnAmountCharged",
              Some(messages("transferIntoOverseasPensions.cya.noTaxOnAmountCharged")).map(Seq(_)),
              routes.PensionSchemeTaxTransferController.show(taxYear)
            )(messages)
        }
      )
    }
  }


  private def summaryRowForSchemesPayingTax(transfersIntoOverseasPensionsViewModel: TransfersIntoOverseasPensionsViewModel, taxYear: Int)
                                           (implicit messages: Messages): Option[SummaryListRow] = {
    transfersIntoOverseasPensionsViewModel.transferPensionSavings.filter(_ == true).flatMap { _ =>
      transfersIntoOverseasPensionsViewModel.overseasTransferCharge.filter(_ == true).flatMap { _ =>
        transfersIntoOverseasPensionsViewModel.pensionSchemeTransferCharge.filter(_ == true).map(_ =>
          summaryListRowWithString(
            "transferIntoOverseasPensions.cya.schemesPayingTax",
            Some(transfersIntoOverseasPensionsViewModel.transferPensionScheme.flatMap(_.name)),
            routes.TransferChargeSummaryController.show(taxYear))(messages)
        )
      }
    }
  }
}
