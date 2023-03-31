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

import models.pension.charges.{Relief, TaxReliefQuestion}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object ReliefsSchemeDetailsHelper extends CYABaseHelper {


  def summaryListRows(relief :Relief, taxYear: Int, index: Option[Int])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      pensionSchemeName(relief, taxYear),
      unTaxedPayerEmployments(relief, taxYear, index),
      typeOfRelief(relief, taxYear, index),
      schemeDetails(relief, taxYear, index)
    ).flatten

  def pensionSchemeName(relief : Relief, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
   Some(relief.customerReferenceNumberQuestion.fold{
      summaryListRowWithString(
        "overseasPension.reliefDetails.pensionSchemeName",
        Some(messages("common.no")).map(Seq(_)),
        routes.PensionsCustomerReferenceNumberController.show(taxYear))
    }{cRNQ =>
      summaryListRowWithString(
        "overseasPension.reliefDetails.pensionSchemeName",
        Some(Seq(cRNQ)),
        routes.PensionsCustomerReferenceNumberController.show(taxYear))
    })
  }

  def unTaxedPayerEmployments(relief : Relief, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {
    Some(relief.employerPaymentsAmount.fold{
      summaryListRowWithString(
        "overseasPension.reliefDetails.amount",
        Some(messages("common.no")).map(Seq(_)),
        routes.UntaxedEmployerPaymentsController.show(taxYear, index))
    }{ePA =>
      summaryListRowWithAmountValue(
        "overseasPension.reliefDetails.amount",
        ePA,
        routes.UntaxedEmployerPaymentsController.show(taxYear, index))
    })
  }


  def typeOfRelief(relief : Relief, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {
    Some(relief.reliefType.fold{
      summaryListRowWithString(
        "overseasPension.reliefDetails.typeOfRelief",
        Some(messages("overseasPension.reliefDetails.noTaxRelief")).map(Seq(_)),
        routes.PensionReliefTypeController.show(taxYear, index))
    }{rT =>
      summaryListRowWithString(
        "overseasPension.reliefDetails.typeOfRelief",
        Some(Seq(rT)),
        routes.PensionReliefTypeController.show(taxYear, index))
    })
  }

  def schemeDetails(relief : Relief, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] = {

    relief.reliefType.flatMap {
      case TaxReliefQuestion.MigrantMemberRelief =>
        Some(summaryListRowWithStrings(
          "overseasPension.reliefDetails.schemeDetail",
          relief.qualifyingOverseasPensionSchemeReferenceNumber,
          routes.QOPSReferenceController.show(taxYear, index)))
      case TaxReliefQuestion.TransitionalCorrespondingRelief =>
        Some(summaryListRowWithStrings(
          "overseasPension.reliefDetails.schemeDetail",
          relief.sf74Reference,
          routes.SF74ReferenceController.show(taxYear)))
      case TaxReliefQuestion.DoubleTaxationRelief =>
        Some(summaryListRowWithStrings(
          "overseasPension.reliefDetails.schemeDetail",
          Some(
            messages("overseasPension.reliefDetails.countryCode") + s" ${relief.alphaTwoCountryCode.getOrElse("")}<br>" +
            messages("overseasPension.reliefDetails.article") + s"  ${relief.doubleTaxationCountryArticle.getOrElse("")}<br>" +
            messages("overseasPension.reliefDetails.treaty") + s" ${relief.doubleTaxationCountryTreaty.getOrElse("")}<br>" +
            messages("overseasPension.reliefDetails.relief") + s" ${displayedValueForOptionalAmount(relief.doubleTaxationReliefAmount)}"
          ),
          routes.ReliefsSchemeDetailsController.show(taxYear, index))) //todo change when new page is added

      case TaxReliefQuestion.NoTaxRelief => Option.empty[SummaryListRow]
    }

  }
}
