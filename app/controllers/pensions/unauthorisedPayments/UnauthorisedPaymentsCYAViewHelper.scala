/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.ViewUtils

object UnauthorisedPaymentsCYAViewHelper {

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

  private def summaryRowForUnauthorisedPaymentQuestion(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    val answerOpt = unauthorisedPaymentsViewModel.noUnauthorisedPaymentQuestion.map(!_)
    Some(
      summaryListRowWithBooleanValue(
        "unauthorisedPayments.common.title",
        answerOpt,
        routes.UnAuthorisedPaymentsController.show(taxYear))(messages)
    )
  }

  private def summaryRowForSurchargeAmount(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.surchargeAmount.map(surchargeAmount =>
      summaryListRowWithAmountValue(
        "unauthorisedPayments.cya.amountSurcharged",
        surchargeAmount,
        routes.SurchargeAmountController.show(taxYear))(messages)
    )
  }

  private def summaryRowForSurchargeTaxAmount(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.surchargeTaxAmount.map(surchargeTaxAmount =>
      summaryListRowWithAmountValue(
        "unauthorisedPayments.cya.nonUkTaxAmountSurcharged",
        surchargeTaxAmount,
        routes.DidYouPayNonUkTaxController.show(taxYear))(messages)
    )
  }

  private def summaryRowForNotSurchargeAmount(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.noSurchargeAmount.map(noSurchargeAmount =>
      summaryListRowWithAmountValue(
        "unauthorisedPayments.cya.amountNotSurcharged",
        noSurchargeAmount,
        routes.NoSurchargeAmountController.show(taxYear))(messages)
    )
  }

  private def summaryRowForNotSurchargeTaxAmount(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.noSurchargeTaxAmount.map(noSurchargeTaxAmount =>
      summaryListRowWithAmountValue(
        "unauthorisedPayments.cya.nonUkTaxAmountNotSurcharged",
        noSurchargeTaxAmount,
        routes.NonUkTaxOnAmountNotSurchargeController.show(taxYear))(messages)
    )
  }

  private def summaryRowForUKPensionSchemes(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] =
    Some(
      summaryListRowWithBooleanValue(
        "unauthorisedPayments.common.ukPensionSchemes",
        unauthorisedPaymentsViewModel.ukPensionSchemesQuestion,
        routes.WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear))(messages)
    )

  private def summaryRowForUKPensionSchemeTaxReferences(unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.filter(_ == true).map(_ =>
      summaryListRowWithString(
        "unauthorisedPayments.cya.pensionSchemeTaxReferences",
        unauthorisedPaymentsViewModel.pensionSchemeTaxReference,
        routes.UnauthorisedPensionSchemeTaxReferenceController.show(taxYear))(messages)
    )
  }

  private def summaryListRowWithBooleanValue(labelMessageKey: String, valueOpt: Option[Boolean], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(valueOpt), changeLink)

  private def summaryListRowWithAmountValue(labelMessageKey: String, value: BigDecimal, changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(value), changeLink)

  private def summaryListRowWithString(labelMessageKey: String, valueOpt: Option[Seq[String]], changeLink: Call)(implicit messages: Messages): SummaryListRow =
    summaryListRow(labelMessageKey, displayedValue(valueOpt), changeLink)


  private def displayedValue(value: BigDecimal): String = if (value == 0) "" else s"£$value"

  private def displayedValue(valueOpt: Option[Boolean])(implicit messages: Messages): String =
    valueOpt.map(value => if (value) messages("common.yes") else messages("common.no")).getOrElse("")

  private def displayedValue(valueOpt: Option[Seq[String]]): String = valueOpt.map(_.mkString(", ")).getOrElse("")


  private def summaryListRow(labelMessageKey: String, displayedValue: String, changeLink: Call)(implicit messages: Messages): SummaryListRow = {
    ViewUtils.summaryListRow(
      HtmlContent(messages(labelMessageKey)),
      HtmlContent(displayedValue),
      actions = Seq(
        (changeLink, messages("common.change"),
          Some(messages(labelMessageKey + ".hidden"))))
    )
  }

}
