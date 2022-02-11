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

package models.pension.reliefs

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PaymentsIntoPensionViewModel(rasPensionPaymentQuestion: Option[Boolean] = None,
                                        totalRASPaymentsAndTaxRelief: Option[BigDecimal] = None,
                                        oneOffRasPaymentPlusTaxReliefQuestion: Option[Boolean] = None,
                                        totalOneOffRasPaymentPlusTaxRelief: Option[BigDecimal] = None,
                                        pensionTaxReliefNotClaimedQuestion: Option[Boolean] = None,
                                        retirementAnnuityContractPaymentsQuestion: Option[Boolean] = None,
                                        totalRetirementAnnuityContractPayments: Option[BigDecimal] = None,
                                        workplacePensionPaymentsQuestion: Option[Boolean] = None,
                                        totalWorkplacePensionPayments: Option[BigDecimal] = None) {

  private def falseOrTrueAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]) = {
    boolField.forall(value => !value || (value && amountField.nonEmpty))
  }

  def isFinished: Boolean = {
    val isDone_rasPensionPaymentQuestion = falseOrTrueAndAmountPopulated(rasPensionPaymentQuestion, totalRASPaymentsAndTaxRelief)
    val isDone_oneOffRASPaymentsQuestion = falseOrTrueAndAmountPopulated(oneOffRasPaymentPlusTaxReliefQuestion, totalOneOffRasPaymentPlusTaxRelief)
    val isDone_retirementAnnuityContractPaymentsQuestion =
      falseOrTrueAndAmountPopulated(retirementAnnuityContractPaymentsQuestion, totalRetirementAnnuityContractPayments)
    val isDone_workplacePensionPaymentsQuestion = falseOrTrueAndAmountPopulated(workplacePensionPaymentsQuestion, totalWorkplacePensionPayments)

    val isDone_taxReliefNotClaimedCompleted = taxReliefNotClaimedCompleted

    Seq(
      isDone_rasPensionPaymentQuestion,
      isDone_oneOffRASPaymentsQuestion,
      isDone_retirementAnnuityContractPaymentsQuestion,
      isDone_workplacePensionPaymentsQuestion,
      isDone_taxReliefNotClaimedCompleted
    ).forall(_ == true)
  }

  def taxReliefNotClaimedCompleted: Boolean = {
    pensionTaxReliefNotClaimedQuestion match {
      case Some(true) =>
        retirementAnnuityContractPaymentsQuestion.contains(true) || workplacePensionPaymentsQuestion.contains(true)
      case Some(false) =>
        retirementAnnuityContractPaymentsQuestion.isEmpty && workplacePensionPaymentsQuestion.isEmpty
      case _ => false
    }
  }
}

object PaymentsIntoPensionViewModel {
  implicit val format: OFormat[PaymentsIntoPensionViewModel] = Json.format[PaymentsIntoPensionViewModel]
}

case class EncryptedPaymentsIntoPensionViewModel(rasPensionPaymentQuestion: Option[EncryptedValue] = None,
                                                 totalRASPaymentsAndTaxRelief: Option[EncryptedValue] = None,
                                                 oneOffRasPaymentPlusTaxReliefQuestion: Option[EncryptedValue] = None,
                                                 totalOneOffRasPaymentPlusTaxRelief: Option[EncryptedValue] = None,
                                                 pensionTaxReliefNotClaimedQuestion: Option[EncryptedValue] = None,
                                                 retirementAnnuityContractPaymentsQuestion: Option[EncryptedValue] = None,
                                                 totalRetirementAnnuityContractPayments: Option[EncryptedValue] = None,
                                                 workplacePensionPaymentsQuestion: Option[EncryptedValue] = None,
                                                 totalWorkplacePensionPayments: Option[EncryptedValue] = None)

object EncryptedPaymentsIntoPensionViewModel {
  implicit val format: OFormat[EncryptedPaymentsIntoPensionViewModel] = Json.format[EncryptedPaymentsIntoPensionViewModel]
}
