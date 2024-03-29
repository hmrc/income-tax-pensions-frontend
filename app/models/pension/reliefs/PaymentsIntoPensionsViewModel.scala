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
import cats.implicits._
import models.pension.AllPensionsData.{Zero, isNotZero}

case class PaymentsIntoPensionsViewModel(rasPensionPaymentQuestion: Option[Boolean] = None,
                                         totalRASPaymentsAndTaxRelief: Option[BigDecimal] = None,
                                         oneOffRasPaymentPlusTaxReliefQuestion: Option[Boolean] = None,
                                         totalOneOffRasPaymentPlusTaxRelief: Option[BigDecimal] = None,
                                         totalPaymentsIntoRASQuestion: Option[Boolean] = None, // This field represents 'Is this correct page'
                                         pensionTaxReliefNotClaimedQuestion: Option[Boolean] = None,
                                         retirementAnnuityContractPaymentsQuestion: Option[Boolean] = None,
                                         totalRetirementAnnuityContractPayments: Option[BigDecimal] = None,
                                         workplacePensionPaymentsQuestion: Option[Boolean] = None,
                                         totalWorkplacePensionPayments: Option[BigDecimal] = None)
    extends PensionCYABaseModel {

  private def yesNoAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]): Boolean =
    boolField.exists(value => !value || (value && amountField.nonEmpty))

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean = {
    val isDone_rasPensionPaymentQuestion = yesNoAndAmountPopulated(rasPensionPaymentQuestion, totalRASPaymentsAndTaxRelief)
    val isDone_oneOffRASPaymentsQuestion =
      rasPensionPaymentQuestion.exists(q =>
        if (q) yesNoAndAmountPopulated(oneOffRasPaymentPlusTaxReliefQuestion, totalOneOffRasPaymentPlusTaxRelief) else true)
    val isDone_totalPaymentsIntoRASQuestion = rasPensionPaymentQuestion.exists(q => if (q) totalPaymentsIntoRASQuestion.contains(true) else true)
    val isDone_taxReliefNotClaimedCompleted = taxReliefNotClaimedQuestionCompleted
    val isDone_retirementAnnuityContractPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(q =>
        if (q) yesNoAndAmountPopulated(retirementAnnuityContractPaymentsQuestion, totalRetirementAnnuityContractPayments) else true)
    val isDone_workplacePensionPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(q =>
        if (q) yesNoAndAmountPopulated(workplacePensionPaymentsQuestion, totalWorkplacePensionPayments) else true)

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

  private def taxReliefNotClaimedQuestionCompleted: Boolean =
    pensionTaxReliefNotClaimedQuestion match {
      case Some(true) =>
        retirementAnnuityContractPaymentsQuestion.exists(x => x) || workplacePensionPaymentsQuestion.exists(x => x)
      case Some(false) =>
        true
      case _ => false
    }

  def toReliefs(overseasPensionSchemeContributions: Option[BigDecimal]): Reliefs =
    Reliefs(
      regularPensionContributions = totalRASPaymentsAndTaxRelief.getOrElse(Zero).some,
      oneOffPensionContributionsPaid = totalOneOffRasPaymentPlusTaxRelief.getOrElse(Zero).some,
      retirementAnnuityPayments = totalRetirementAnnuityContractPayments.getOrElse(Zero).some,
      paymentToEmployersSchemeNoTaxRelief = totalWorkplacePensionPayments.getOrElse(Zero).some,
      overseasPensionSchemeContributions = overseasPensionSchemeContributions // not part of this journey, but if we set None it will be wiped out
    )

  def updatePensionTaxReliefNotClaimedQuestion(newValue: Boolean) = {
    val valueNotChanged = pensionTaxReliefNotClaimedQuestion.contains(newValue)

    copy(
      pensionTaxReliefNotClaimedQuestion = Some(newValue),
      retirementAnnuityContractPaymentsQuestion = if (valueNotChanged) retirementAnnuityContractPaymentsQuestion else None,
      totalRetirementAnnuityContractPayments = if (valueNotChanged) totalRetirementAnnuityContractPayments else None,
      workplacePensionPaymentsQuestion = if (valueNotChanged) workplacePensionPaymentsQuestion else None,
      totalWorkplacePensionPayments = if (valueNotChanged) totalWorkplacePensionPayments else None
    )
  }

}

object PaymentsIntoPensionsViewModel {
  implicit val format: OFormat[PaymentsIntoPensionsViewModel] = Json.format[PaymentsIntoPensionsViewModel]

  def empty: PaymentsIntoPensionsViewModel =
    PaymentsIntoPensionsViewModel(None, None, None, None, None, None, None, None, None, None)

  def fromSubmittedReliefs(reliefs: Reliefs): PaymentsIntoPensionsViewModel = {
    val rasPensionPaymentQuestion: Boolean             = isNotZero(reliefs.regularPensionContributions)
    val totalRASPaymentsAndTaxRelief: BigDecimal       = reliefs.regularPensionContributions.getOrElse(Zero)
    val oneOffRasPaymentPlusTaxReliefQuestion: Boolean = isNotZero(reliefs.oneOffPensionContributionsPaid)
    val totalOneOffRasPaymentPlusTaxRelief: BigDecimal = reliefs.oneOffPensionContributionsPaid.getOrElse(Zero)

    val pensionTaxReliefNotClaimedQuestion: Boolean =
      isNotZero(reliefs.retirementAnnuityPayments) || isNotZero(reliefs.paymentToEmployersSchemeNoTaxRelief)

    if (pensionTaxReliefNotClaimedQuestion) {
      PaymentsIntoPensionsViewModel(
        rasPensionPaymentQuestion = rasPensionPaymentQuestion.some,
        totalRASPaymentsAndTaxRelief = totalRASPaymentsAndTaxRelief.some,
        oneOffRasPaymentPlusTaxReliefQuestion = oneOffRasPaymentPlusTaxReliefQuestion.some,
        totalOneOffRasPaymentPlusTaxRelief = totalOneOffRasPaymentPlusTaxRelief.some,
        totalPaymentsIntoRASQuestion = Some(true), // It must be true for 'Is this correct' when reaching CYA
        pensionTaxReliefNotClaimedQuestion = pensionTaxReliefNotClaimedQuestion.some,
        retirementAnnuityContractPaymentsQuestion = isNotZero(reliefs.retirementAnnuityPayments).some,
        totalRetirementAnnuityContractPayments = reliefs.retirementAnnuityPayments.getOrElse(Zero).some,
        workplacePensionPaymentsQuestion = isNotZero(reliefs.paymentToEmployersSchemeNoTaxRelief).some,
        totalWorkplacePensionPayments = reliefs.paymentToEmployersSchemeNoTaxRelief.getOrElse(Zero).some
      )
    } else {
      PaymentsIntoPensionsViewModel.empty.copy(
        rasPensionPaymentQuestion = rasPensionPaymentQuestion.some,
        totalRASPaymentsAndTaxRelief = totalRASPaymentsAndTaxRelief.some,
        oneOffRasPaymentPlusTaxReliefQuestion = oneOffRasPaymentPlusTaxReliefQuestion.some,
        totalOneOffRasPaymentPlusTaxRelief = totalOneOffRasPaymentPlusTaxRelief.some,
        totalPaymentsIntoRASQuestion = Some(true), // It must be true for 'Is this correct' when reaching CYA
        pensionTaxReliefNotClaimedQuestion = pensionTaxReliefNotClaimedQuestion.some,
        retirementAnnuityContractPaymentsQuestion = Some(false),
        totalRetirementAnnuityContractPayments = Zero.some,
        workplacePensionPaymentsQuestion = Some(false),
        totalWorkplacePensionPayments = Zero.some
      )
    }

  }
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
