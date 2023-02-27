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

package controllers.pensions.shortServiceRefunds

import models.pension.charges.ShortServiceRefundsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object ShortSummaryCYAViewHelper extends CYABaseHelper {


  def summaryListRows(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForShortServiceRefund(shortServiceRefundsViewModel, taxYear),
      summaryRowForShortServiceRefundsAmount(shortServiceRefundsViewModel, taxYear),
      summaryRowForShortServiceRefundCharge(shortServiceRefundsViewModel, taxYear),
      summaryRowForShortServiceRefundChargeAmount(shortServiceRefundsViewModel, taxYear),
      summaryRowForShortServiceSchemes(shortServiceRefundsViewModel, taxYear)
    ).flatten

  private def summaryRowForShortServiceRefund(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)
                                                       (implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "shortServiceRefunds.cya.refund",
        shortServiceRefundsViewModel.shortServiceRefund,
        routes.TaxableRefundAmountController.show(taxYear))(messages)
    )
  }

  private def summaryRowForShortServiceRefundsAmount(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)
                                        (implicit messages: Messages): Option[SummaryListRow] = {
    shortServiceRefundsViewModel.shortServiceRefund
      .filter(_ == true)
      .flatMap(_ =>
        shortServiceRefundsViewModel.shortServiceRefund match {
          case Some(true) if shortServiceRefundsViewModel.shortServiceRefundCharge.isDefined =>
            Some(summaryListRowWithOptionalAmountValue(
              "shortServiceRefunds.cya.refundAmount",
              shortServiceRefundsViewModel.shortServiceRefundCharge,
              routes.TaxableRefundAmountController.show(taxYear))(messages))
          case _ => Option.empty[SummaryListRow]
        }
      )
  }

  private def summaryRowForShortServiceRefundCharge(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)
                                              (implicit messages: Messages): Option[SummaryListRow] = {
    shortServiceRefundsViewModel.shortServiceRefundTaxPaid match {
      case Some(true) => Some(summaryListRowWithBooleanValue(
        "shortServiceRefunds.cya.nonUk",
        shortServiceRefundsViewModel.shortServiceRefundTaxPaid,
        routes.NonUkTaxRefundsController.show(taxYear))(messages))
      case _ => Some(summaryListRowWithString(
        "shortServiceRefunds.cya.nonUk",
        Some(messages("common.noTaxPaid")).map(Seq(_)),
        routes.NonUkTaxRefundsController.show(taxYear))(messages))
    }
  }

  private def summaryRowForShortServiceRefundChargeAmount(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)
                                                    (implicit messages: Messages): Option[SummaryListRow] = {
    shortServiceRefundsViewModel.shortServiceRefundTaxPaid
      .filter(_ == true)
      .flatMap(_ =>
        shortServiceRefundsViewModel.shortServiceRefundTaxPaid match {
          case Some(true) if shortServiceRefundsViewModel.shortServiceRefundTaxPaidCharge.isDefined =>
            Some(summaryListRowWithOptionalAmountValue(
              "shortServiceRefunds.cya.nonUkAmount",
              shortServiceRefundsViewModel.shortServiceRefundTaxPaidCharge,
              routes.NonUkTaxRefundsController.show(taxYear))(messages))
          case _ => Option.empty[SummaryListRow]
        }
      )
  }

  private def summaryRowForShortServiceSchemes(shortServiceRefundsViewModel: ShortServiceRefundsViewModel, taxYear: Int)
                                           (implicit messages: Messages): Option[SummaryListRow] = {
        if (shortServiceRefundsViewModel.refundPensionScheme.isEmpty){
          Option.empty[SummaryListRow]
        } else {
          Some(summaryListRowWithString(
            "shortServiceRefunds.cya.schemesPayingTax",
            Some(shortServiceRefundsViewModel.refundPensionScheme.flatMap(_.name)),
            routes.RefundSummaryController.show(taxYear))(messages)
        )}
      }
}
