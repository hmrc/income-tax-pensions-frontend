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

import cats.implicits._
import connectors.OptionalContentHttpReads
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData.{Zero, isNotZero}
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PaymentsIntoPensionsViewModel(
    rasPensionPaymentQuestion: Option[Boolean] = None,
    totalRASPaymentsAndTaxRelief: Option[BigDecimal] = None,
    oneOffRasPaymentPlusTaxReliefQuestion: Option[Boolean] = None,
    totalOneOffRasPaymentPlusTaxRelief: Option[BigDecimal] = None,
    totalPaymentsIntoRASQuestion: Option[Boolean] =
      None, // This field represents 'Is this correct page'. When Redirect service is overhauled we can remove this question data
    pensionTaxReliefNotClaimedQuestion: Option[Boolean] = None,
    retirementAnnuityContractPaymentsQuestion: Option[Boolean] = None,
    totalRetirementAnnuityContractPayments: Option[BigDecimal] = None,
    workplacePensionPaymentsQuestion: Option[Boolean] = None,
    totalWorkplacePensionPayments: Option[BigDecimal] = None
) extends PensionCYABaseModel {

  private def yesNoAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]): Boolean =
    boolField.exists(value => !value || (value && amountField.nonEmpty))

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean = {
    val isDone_rasPensionPaymentQuestion = yesNoAndAmountPopulated(rasPensionPaymentQuestion, totalRASPaymentsAndTaxRelief)
    val isDone_oneOffRASPaymentsQuestion = rasPensionPaymentQuestion.exists(rasPensionAnswer =>
      if (rasPensionAnswer) yesNoAndAmountPopulated(oneOffRasPaymentPlusTaxReliefQuestion, totalOneOffRasPaymentPlusTaxRelief) else true)
    val isDone_totalPaymentsIntoRASQuestion =
      rasPensionPaymentQuestion.exists(rasPensionAnswer => if (rasPensionAnswer) totalPaymentsIntoRASQuestion.contains(true) else true)
    val isDone_taxReliefNotClaimedCompleted =
      pensionTaxReliefNotClaimedQuestion.isDefined // a helper question, even if it's Done, we need to check following questions
    val isDone_retirementAnnuityContractPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(reliefNotClaimedAnswer =>
        if (reliefNotClaimedAnswer) yesNoAndAmountPopulated(retirementAnnuityContractPaymentsQuestion, totalRetirementAnnuityContractPayments)
        else true)
    val isDone_workplacePensionPaymentsQuestion =
      pensionTaxReliefNotClaimedQuestion.exists(reliefNotClaimedAnswer =>
        if (reliefNotClaimedAnswer) yesNoAndAmountPopulated(workplacePensionPaymentsQuestion, totalWorkplacePensionPayments) else true)

    List(
      isDone_rasPensionPaymentQuestion,
      isDone_oneOffRASPaymentsQuestion,
      isDone_totalPaymentsIntoRASQuestion,
      isDone_taxReliefNotClaimedCompleted,
      isDone_retirementAnnuityContractPaymentsQuestion,
      isDone_workplacePensionPaymentsQuestion,
    ).forall(identity)
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

  def updateRasPensionPaymentQuestion(yesAnswer: Boolean): PaymentsIntoPensionsViewModel =
    if (yesAnswer) {
      copy(rasPensionPaymentQuestion = Some(true))
    } else {
      copy(
        rasPensionPaymentQuestion = Some(false),
        totalRASPaymentsAndTaxRelief = None,
        oneOffRasPaymentPlusTaxReliefQuestion = None,
        totalOneOffRasPaymentPlusTaxRelief = None,
        totalPaymentsIntoRASQuestion = None
      )
    }

  def updatePensionTaxReliefNotClaimedQuestion(yesAnswer: Boolean): PaymentsIntoPensionsViewModel =
    if (yesAnswer) {
      copy(pensionTaxReliefNotClaimedQuestion = Some(true))
    } else {
      copy(
        pensionTaxReliefNotClaimedQuestion = Some(false),
        retirementAnnuityContractPaymentsQuestion = None,
        totalRetirementAnnuityContractPayments = None,
        workplacePensionPaymentsQuestion = None,
        totalWorkplacePensionPayments = None
      )
    }

  // We mark the helper question (totalPaymentsIntoRASQuestion) as true (as it must have been correct to reach CYA)
  def toPensionsCYAModel: PensionsCYAModel =
    PensionsCYAModel.emptyModels.copy(paymentsIntoPension = this.copy(totalPaymentsIntoRASQuestion = Some(true)))

}

object PaymentsIntoPensionsViewModel {
  implicit val format: OFormat[PaymentsIntoPensionsViewModel]                  = Json.format[PaymentsIntoPensionsViewModel]
  implicit val optRds: OptionalContentHttpReads[PaymentsIntoPensionsViewModel] = new OptionalContentHttpReads[PaymentsIntoPensionsViewModel]

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
