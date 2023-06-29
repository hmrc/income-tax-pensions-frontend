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

package models.pension.reliefs

import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PaymentsIntoPensionsViewModel(rasPensionPaymentQuestion: Option[Boolean] = None,
                                         totalRASPaymentsAndTaxRelief: Option[BigDecimal] = None,
                                         oneOffRasPaymentPlusTaxReliefQuestion: Option[Boolean] = None,
                                         totalOneOffRasPaymentPlusTaxRelief: Option[BigDecimal] = None,
                                         totalPaymentsIntoRASQuestion: Option[Boolean] = None,
                                         pensionTaxReliefNotClaimedQuestion: Option[Boolean] = None,
                                         retirementAnnuityContractPaymentsQuestion: Option[Boolean] = None,
                                         totalRetirementAnnuityContractPayments: Option[BigDecimal] = None,
                                         workplacePensionPaymentsQuestion: Option[Boolean] = None,
                                         totalWorkplacePensionPayments: Option[BigDecimal] = None) extends PensionCYABaseModel {

  private def yesNoAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]): Boolean = {
    boolField.exists(value => !value || (value && amountField.nonEmpty))
  }

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def isFinished: Boolean = {
    val isDone_rasPensionPaymentQuestion = yesNoAndAmountPopulated(rasPensionPaymentQuestion, totalRASPaymentsAndTaxRelief)
    val isDone_oneOffRASPaymentsQuestion =
      rasPensionPaymentQuestion.exists(q =>
        if(q) yesNoAndAmountPopulated(oneOffRasPaymentPlusTaxReliefQuestion, totalOneOffRasPaymentPlusTaxRelief) else true
    )
    val isDone_totalPaymentsIntoRASQuestion = rasPensionPaymentQuestion.exists(q => if(q) totalPaymentsIntoRASQuestion.contains(true) else true)
    val isDone_taxReliefNotClaimedCompleted = taxReliefNotClaimedQuestionCompleted
    val isDone_retirementAnnuityContractPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(q =>
        if(q) yesNoAndAmountPopulated(retirementAnnuityContractPaymentsQuestion, totalRetirementAnnuityContractPayments) else true
      )
    val isDone_workplacePensionPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(q =>
        if(q) yesNoAndAmountPopulated(workplacePensionPaymentsQuestion, totalWorkplacePensionPayments) else true
      )

    Seq(
      isDone_rasPensionPaymentQuestion,
      isDone_oneOffRASPaymentsQuestion,
      isDone_totalPaymentsIntoRASQuestion,
      isDone_retirementAnnuityContractPaymentsQuestion,
      isDone_workplacePensionPaymentsQuestion,
      isDone_taxReliefNotClaimedCompleted
    ).forall(x => x)
  }

  def journeyIsNo: Boolean =
    (!rasPensionPaymentQuestion.getOrElse(true)
      && totalRASPaymentsAndTaxRelief.isEmpty
      && oneOffRasPaymentPlusTaxReliefQuestion.isEmpty
      && totalOneOffRasPaymentPlusTaxRelief.isEmpty
      && totalPaymentsIntoRASQuestion.isEmpty
      && pensionTaxReliefNotClaimedQuestion.isEmpty
      && retirementAnnuityContractPaymentsQuestion.isEmpty
      && totalRetirementAnnuityContractPayments.isEmpty
      && workplacePensionPaymentsQuestion.isEmpty
      && totalWorkplacePensionPayments.isEmpty)

  def journeyIsUnanswered: Boolean = this.productIterator.forall(_ == None)

  private def taxReliefNotClaimedQuestionCompleted: Boolean = {
    pensionTaxReliefNotClaimedQuestion match {
      case Some(true) =>
        retirementAnnuityContractPaymentsQuestion.exists(x => x)|| workplacePensionPaymentsQuestion.exists(x => x)
      case Some(false) =>
        retirementAnnuityContractPaymentsQuestion.isEmpty && workplacePensionPaymentsQuestion.isEmpty
      case _ => false
    }
  }
}

object PaymentsIntoPensionsViewModel {
  implicit val format: OFormat[PaymentsIntoPensionsViewModel] = Json.format[PaymentsIntoPensionsViewModel]
}

case class EncryptedPaymentsIntoPensionViewModel(rasPensionPaymentQuestion: Option[EncryptedValue] = None,
                                                 totalRASPaymentsAndTaxRelief: Option[EncryptedValue] = None,
                                                 oneOffRasPaymentPlusTaxReliefQuestion: Option[EncryptedValue] = None,
                                                 totalOneOffRasPaymentPlusTaxRelief: Option[EncryptedValue] = None,
                                                 totalPaymentsIntoRASQuestion: Option[EncryptedValue] = None,
                                                 pensionTaxReliefNotClaimedQuestion: Option[EncryptedValue] = None,
                                                 retirementAnnuityContractPaymentsQuestion: Option[EncryptedValue] = None,
                                                 totalRetirementAnnuityContractPayments: Option[EncryptedValue] = None,
                                                 workplacePensionPaymentsQuestion: Option[EncryptedValue] = None,
                                                 totalWorkplacePensionPayments: Option[EncryptedValue] = None)

object EncryptedPaymentsIntoPensionViewModel {
  implicit val format: OFormat[EncryptedPaymentsIntoPensionViewModel] = Json.format[EncryptedPaymentsIntoPensionViewModel]
}

