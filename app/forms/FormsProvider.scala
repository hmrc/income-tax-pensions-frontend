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

package forms

import forms.OptionalTupleAmountForm.OptionalTupleAmountFormErrorMessage
import models.User
import models.pension.charges.TaxReliefQuestion
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class FormsProvider() {
  def overseasTransferChargePaidForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "transferIntoOverseasPensions.overseasTransferChargesPaid.error.noEntry"
  )

  def shortServiceTaxOnShortServiceRefundForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "shortServiceRefunds.taxOnShortServiceRefund.error.noEntry"
  )

  def pensionSchemeTaxTransferForm(user:User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.tooBig"
    )
  }

  def shortServiceTaxableRefundForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"shortServiceRefunds.taxableRefundAmount.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"shortServiceRefunds.taxableRefundAmount.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"shortServiceRefunds.taxableRefundAmount.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"shortServiceRefunds.taxableRefundAmount.error.tooBig.$agentOrIndividual"
    )
  }

  def nonUkTaxRefundsForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"shortServiceRefunds.nonUkTaxRefunds.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"shortServiceRefunds.nonUkTaxRefunds.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"shortServiceRefunds.nonUkTaxRefunds.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"shortServiceRefunds.nonUkTaxRefunds.error.tooBig.$agentOrIndividual"
    )
  }

  lazy val pensionAmountForm: Form[(Option[BigDecimal], Option[BigDecimal])] = {
    OptionalTupleAmountForm.amountForm(OptionalTupleAmountFormErrorMessage(
      emptyFieldKey1 = "pensions.pensionAmount.totalTax.error.noEntry",
      wrongFormatKey1 = s"pensions.pensionAmount.totalTax.error.incorrectFormat",
      exceedsMaxAmountKey1 = s"pensions.pensionAmount.totalTax.error.overMaximum",
      emptyFieldKey2 = s"pensions.pensionAmount.taxPaid.error.noEntry",
      wrongFormatKey2 = s"pensions.pensionAmount.taxPaid.error.incorrectFormat",
      exceedsMaxAmountKey2 = s"pensions.pensionAmount.taxPaid.error.overMaximum"
    ))
  }

  def pensionPaymentsForm(user: User): Form[(Option[BigDecimal], Option[BigDecimal])] = {
    OptionalTupleAmountForm.amountForm(OptionalTupleAmountFormErrorMessage(
      emptyFieldKey1 = "overseasPension.pensionPayments.amountBeforeTax.noEntry",
      wrongFormatKey1 = s"overseasPension.pensionPayments.amountBeforeTax.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
      exceedsMaxAmountKey1 = "overseasPension.pensionPayments.amountBeforeTax.tooBig",
      emptyFieldKey2 = "common.pensions.error.amount.noEntry",
      wrongFormatKey2 = "overseasPension.pensionPayments.nonUkTaxPaid.incorrectFormat",
      exceedsMaxAmountKey2 = "common.pensions.error.amount.overMaximum",
      taxPaidLessThanAmountBeforeTaxErrorMessage = "overseasPension.pensionPayments.nonUkTaxPaidLessThanAmountBeforeTax"
    ))
  }

  def statePensionForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.statePension.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"pensions.statePension.amount.error.noEntry.$agentOrIndividual",
      wrongFormatKey = s"pensions.statePension.amount.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"pensions.statePension.amount.error.overMaximum.$agentOrIndividual"
    )
  }

  def pensionTakenAnotherWayAmountForm(isAgent: Boolean): Form[(Option[BigDecimal], Option[BigDecimal])] = {
    OptionalTupleAmountForm.amountForm(OptionalTupleAmountFormErrorMessage(
      emptyFieldKey1 = s"lifetimeAllowance.pensionTakenAnotherWay.beforeTax.error.noEntry.${if (isAgent) "agent" else "individual"}",
      wrongFormatKey1 = s"lifetimeAllowance.pensionTakenAnotherWay.beforeTax.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
      exceedsMaxAmountKey1 = s"common.beforeTax.error.overMaximum",
      emptyFieldKey2 = s"lifetimeAllowance.pensionTakenAnotherWay.taxPaid.error.noEntry.${if (isAgent) "agent" else "individual"}",
      wrongFormatKey2 = s"common.taxPaid.error.incorrectFormat",
      exceedsMaxAmountKey2 = s"common.taxPaid.error.overMaximum"
    ))
  }

  def untaxedEmployerPayments(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"overseasPension.untaxedEmployerPayments.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"overseasPension.untaxedEmployerPayments.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"overseasPension.untaxedEmployerPayments.error.tooBig.${if (isAgent) "agent" else "individual"}"
  )

  def sf74ReferenceIdForm: Form[String] = SF74ReferenceForm.sf74ReferenceIdForm(
    noEntryMsg = "pensions.paymentsIntoOverseasPensions.sf74Reference.noEntry",
    incorrectFormatMsg = "pensions.paymentsIntoOverseasPensions.sf74Reference.incorrectFormat"
  )

  def overseasPensionsReliefTypeForm: Form[String] = {
    RadioButtonForm.radioButtonForm("overseasPension.pensionReliefType.error.noEntry", TaxReliefQuestion.validTaxList)
  }

  def taxPaidOnStatePensionLumpSum(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.taxPaidOnStatePensionLumpSum.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"pensions.taxPaidOnStatePensionLumpSum.amount.error.noEntry.$agentOrIndividual",
      wrongFormatKey = s"pensions.taxPaidOnStatePensionLumpSum.amount.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"pensions.taxPaidOnStatePensionLumpSum.amount.error.overMaximum.$agentOrIndividual"
    )
  }

  def stateBenefitDateForm: Form[DateForm.DateModel] = {
    DateForm.dateForm("stateBenefitStartDate", "incomeFromPensions.stateBenefitStartDate")
  }

  def pensionSchemeDateForm: Form[DateForm.DateModel] = {
    DateForm.dateForm("pensionStartDate", "incomeFromPensions.pensionStartDate")
  }

}
