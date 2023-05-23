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

package controllers.pensions.paymentsIntoOverseasPensions

import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object PaymentsIntoOverseasPensionCYAViewHelper extends CYABaseHelper {

  def summaryListRows( piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                     (implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForPiop(piopViewModel, taxYear),
      summaryRowForTotalPayments(piopViewModel, taxYear),
      summaryRowForEmployerPayments(piopViewModel, taxYear),
      summaryRowForEmployerPaymentsTax(piopViewModel, taxYear),
      summaryRowForOverseasSchemes(piopViewModel, taxYear)
    ).flatten

  private def summaryRowForPiop(piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                                                       (implicit messages: Messages): Option[SummaryListRow] =
    Some(
      summaryListRowWithBooleanValue(
        "pensions.pensionSummary.paymentsToOverseasPensions",
        piopViewModel.paymentsIntoOverseasPensionsQuestions,
        routes.PaymentIntoPensionSchemeController.show(taxYear))(messages)
    )

  private def summaryRowForTotalPayments(piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                                        (implicit messages: Messages): Option[SummaryListRow] =
    piopViewModel.paymentsIntoOverseasPensionsQuestions
      .filter(x => x)
      .flatMap(_ =>
        piopViewModel.paymentsIntoOverseasPensionsAmount
          .map(_ =>
            summaryListRowWithOptionalAmountValue(
              "paymentsIntoOverseasPensions.cya.totalPayments",
              piopViewModel.paymentsIntoOverseasPensionsAmount,
              routes.PaymentIntoPensionSchemeController.show(taxYear))(messages)
          )
      )

  private def summaryRowForEmployerPayments(piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                                             (implicit messages: Messages): Option[SummaryListRow] =
    Some(
      summaryListRowWithBooleanValue(
        "paymentsIntoOverseasPensions.cya.employerPayments",
        piopViewModel.employerPaymentsQuestion,
        routes.EmployerPayOverseasPensionController.show(taxYear))(messages)
    )

  private def summaryRowForEmployerPaymentsTax(piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                                           (implicit messages: Messages): Option[SummaryListRow] =
    piopViewModel.employerPaymentsQuestion
      .filter(x => x)
      .map(_ =>
          summaryListRowWithBooleanValue(
            "paymentsIntoOverseasPensions.cya.employerPaymentsTax",
            piopViewModel.taxPaidOnEmployerPaymentsQuestion,
            routes.TaxEmployerPaymentsController.show(taxYear))(messages)
        )

  private def summaryRowForOverseasSchemes(piopViewModel: PaymentsIntoOverseasPensionsViewModel, taxYear: Int)
                                           (implicit messages: Messages): Option[SummaryListRow] = {
    piopViewModel.employerPaymentsQuestion.filter(x => x).flatMap(_ => {
      piopViewModel.taxPaidOnEmployerPaymentsQuestion
        .filterNot(x =>x)
        .map(_ =>
          summaryListRowWithStrings(
            "common.overseas.pension.schemes",
            Some(piopViewModel.reliefs.flatMap(_.customerReference)),
            routes.ReliefsSchemeSummaryController.show(taxYear))(messages)
        )
    })
  }
}
