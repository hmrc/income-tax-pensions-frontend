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

import models.pension.charges.{OverseasPensionScheme, TaxReliefQuestion}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object ReliefsSchemeDetailsHelper extends CYABaseHelper {

  def summaryListRows(relief: OverseasPensionScheme, taxYear: Int, index: Option[Int])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      pensionSchemeName(relief, taxYear, index),
      unTaxedPayerEmployments(relief, taxYear, index),
      typeOfRelief(relief, taxYear, index),
      schemeDetails(relief, taxYear, index)
    ).flatten

  def pensionSchemeName(relief: OverseasPensionScheme, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] =
    Some(relief.customerReference.fold {
      summaryListRowWithString(
        "overseasPension.reliefDetails.pensionSchemeName",
        Some(messages("common.no")),
        routes.PensionsCustomerReferenceNumberController.show(taxYear, index))
    } { cRNQ =>
      summaryListRowWithString(
        "overseasPension.reliefDetails.pensionSchemeName",
        Some(cRNQ),
        routes.PensionsCustomerReferenceNumberController.show(taxYear, index))
    })

  private def unTaxedPayerEmployments(relief: OverseasPensionScheme, taxYear: Int, index: Option[Int])(implicit
      messages: Messages): Option[SummaryListRow] =
    Some(relief.employerPaymentsAmount.fold {
      summaryListRowWithString(
        "overseasPension.reliefDetails.amount",
        Some(messages("common.no")),
        routes.UntaxedEmployerPaymentsController.show(taxYear, index))
    } { ePA =>
      summaryListRowWithAmountValue("overseasPension.reliefDetails.amount", ePA, routes.UntaxedEmployerPaymentsController.show(taxYear, index))
    })

  def typeOfRelief(relief: OverseasPensionScheme, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] =
    Some(relief.reliefType.fold {
      summaryListRowWithString(
        "overseasPension.reliefDetails.typeOfRelief",
        Some(messages("overseasPension.reliefDetails.noTaxRelief")),
        routes.PensionReliefTypeController.show(taxYear, index)
      )
    } { rT =>
      summaryListRowWithString(
        "overseasPension.reliefDetails.typeOfRelief",
        Some(typeOfReliefToMessage(rT)),
        routes.PensionReliefTypeController.show(taxYear, index))
    })

  private def typeOfReliefToMessage(reliefType: String)(implicit messages: Messages): String =
    reliefType match {
      case TaxReliefQuestion.DoubleTaxationRelief            => messages("overseasPension.pensionReliefType.DTR")
      case TaxReliefQuestion.MigrantMemberRelief             => messages("overseasPension.pensionReliefType.MMR")
      case TaxReliefQuestion.TransitionalCorrespondingRelief => messages("overseasPension.pensionReliefType.TCR")
      case _                                                 => messages("overseasPension.reliefDetails.noTaxRelief")
    }

  def schemeDetails(relief: OverseasPensionScheme, taxYear: Int, index: Option[Int])(implicit messages: Messages): Option[SummaryListRow] =
    relief.reliefType.flatMap {
      case TaxReliefQuestion.MigrantMemberRelief =>
        Some(
          summaryListRowWithString(
            "overseasPension.reliefDetails.schemeDetail",
            relief.qopsReference,
            routes.QOPSReferenceController.show(taxYear, index)))
      case TaxReliefQuestion.TransitionalCorrespondingRelief =>
        Some(
          summaryListRowWithString(
            "overseasPension.reliefDetails.schemeDetail",
            relief.sf74Reference,
            routes.SF74ReferenceController.show(taxYear, index)))
      case TaxReliefQuestion.DoubleTaxationRelief =>
        Some(
          summaryListRowWithStrings(
            "overseasPension.reliefDetails.schemeDetail",
            Some(
              Seq(
                messages("overseasPension.reliefDetails.countryCode") + s" ${relief.alphaTwoCountryCode.getOrElse("")}<br>" +
                  messages("overseasPension.reliefDetails.article") + s"  ${relief.doubleTaxationArticle.getOrElse("")}<br>" +
                  messages("overseasPension.reliefDetails.treaty") + s" ${relief.doubleTaxationTreaty.getOrElse("")}<br>" +
                  messages("overseasPension.reliefDetails.relief") + s" ${displayedValueForOptionalAmount(relief.doubleTaxationReliefAmount)}"
              )),
            routes.DoubleTaxationAgreementController.show(taxYear, index)
          ))

      case _ => Option.empty[SummaryListRow]
    }

}
