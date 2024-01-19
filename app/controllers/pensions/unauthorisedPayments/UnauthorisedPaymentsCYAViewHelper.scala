/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.pensions.unauthorisedPayments

import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object UnauthorisedPaymentsCYAViewHelper extends CYABaseHelper {

  def summaryListRows(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForUnauthorisedPaymentQuestion(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForSurchargeAmount(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForSurchargeTaxAmount(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForNotSurchargeAmount(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForNotSurchargeTaxAmount(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForUKPensionSchemes(unauthorisedPaymentsViewModel, taxYear),
      summaryRowForUKPensionSchemeTaxReferences(unauthorisedPaymentsViewModel, taxYear)
    ).flatten

  private def summaryRowForUnauthorisedPaymentQuestion(
                                                        unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                                        taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "unauthorisedPayments.common.title",
        unauthorisedPaymentsViewModel.unauthorisedPaymentQuestion,
        routes.UnauthorisedPaymentsController.show(taxYear))(messages)
    )
  }

  private def summaryRowForSurchargeAmount(
                                            unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                            taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.surchargeQuestion
      .filter(_ == true)
      .map(_ =>
        summaryListRowWithOptionalAmountValue(
          "unauthorisedPayments.cya.amountSurcharged",
          unauthorisedPaymentsViewModel.surchargeAmount.fold(Some(BigDecimal(0)))(item => Some(item)),
          routes.SurchargeAmountController.show(taxYear))(messages)
      )
  }

  private def summaryRowForSurchargeTaxAmount(
                                               unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                               taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.surchargeQuestion
      .filter(_ == true)
      .map(_ =>
        summaryListRowWithOptionalAmountValue(
          "unauthorisedPayments.cya.nonUkTaxAmountSurcharged",
          unauthorisedPaymentsViewModel.surchargeTaxAmount.fold(Some(BigDecimal(0)))(item => Some(item)),
          controllers.pensions.unauthorisedPayments.routes.NonUKTaxOnAmountResultedInSurchargeController.show(taxYear))(messages)
      )
  }

  private def summaryRowForNotSurchargeAmount(
                                               unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                               taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.noSurchargeQuestion
      .filter(_ == true)
      .map(_ =>
        summaryListRowWithOptionalAmountValue(
          "unauthorisedPayments.cya.amountNotSurcharged",
          unauthorisedPaymentsViewModel.noSurchargeAmount.fold(Some(BigDecimal(0)))(item => Some(item)),
          routes.NoSurchargeAmountController.show(taxYear))(messages)
      )
  }

  private def summaryRowForNotSurchargeTaxAmount(
                                                  unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                                  taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    unauthorisedPaymentsViewModel.noSurchargeQuestion
      .filter(_ == true)
      .map(_ =>
        summaryListRowWithOptionalAmountValue(
          "unauthorisedPayments.cya.nonUkTaxAmountNotSurcharged",
          unauthorisedPaymentsViewModel.noSurchargeTaxAmount.fold(Some(BigDecimal(0)))(item => Some(item)),
          routes.NonUKTaxOnAmountNotResultedInSurchargeController.show(taxYear))(messages)
      )

  private def summaryRowForUKPensionSchemes(
                                             unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                             taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    unauthorisedPaymentsViewModel.unauthorisedPaymentQuestion.filter(_ == true).map(_ =>
      summaryListRowWithBooleanValue(
        "unauthorisedPayments.common.ukPensionSchemes",
        unauthorisedPaymentsViewModel.ukPensionSchemesQuestion,
        routes.WereAnyOfTheUnauthorisedPaymentsController.show(taxYear))(messages)
    )

  private def summaryRowForUKPensionSchemeTaxReferences(
                                                         unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                                                         taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.unauthorisedPaymentQuestion.filter(_ == true).flatMap(_ =>
      unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.filter(_ == true).map(_ =>
        summaryListRowWithStrings(
          "unauthorisedPayments.cya.pensionSchemeTaxReferences",
          unauthorisedPaymentsViewModel.pensionSchemeTaxReference,
          routes.UkPensionSchemeDetailsController.show(taxYear))(messages)
      )
    )
  }
}
