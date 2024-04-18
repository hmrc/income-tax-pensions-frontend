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

import forms.FormsProvider.userType
import forms.OptionalTupleAmountForm.OptionalTupleAmountFormErrorMessage
import models.User
import models.pension.charges.TaxReliefQuestion
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class FormsProvider {

  def sectionCompletedStateForm: Form[Boolean] =
    YesNoForm.yesNoForm("sectionCompletedState.error.required")

  def reducedAnnualAllowanceForm(user: User): Form[Boolean] = {
    val agentOrIndividual = userType(user.isAgent)
    YesNoForm.yesNoForm(missingInputError = s"annualAllowance.reducedAnnualAllowance.error.noEntry.$agentOrIndividual")
  }

  def overseasTransferChargePaidForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "transferIntoOverseasPensions.overseasTransferChargesPaid.error.noEntry"
  )

  def pensionSchemeTaxTransferForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.tooBig"
    )
  }

  def shortServiceTaxableRefundForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"shortServiceRefunds.taxableRefundAmount.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"shortServiceRefunds.taxableRefundAmount.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"shortServiceRefunds.taxableRefundAmount.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"shortServiceRefunds.taxableRefundAmount.error.tooBig.$agentOrIndividual"
    )
  }

  def nonUkTaxRefundsForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"shortServiceRefunds.nonUkTaxRefunds.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"shortServiceRefunds.nonUkTaxRefunds.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"shortServiceRefunds.nonUkTaxRefunds.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"shortServiceRefunds.nonUkTaxRefunds.error.tooBig.$agentOrIndividual"
    )
  }

  def pensionAmountForm(user: User): Form[(Option[BigDecimal], Option[BigDecimal])] = {
    val agentIndividual = if (user.isAgent) "agent" else "individual"
    OptionalTupleAmountForm.amountForm(
      OptionalTupleAmountFormErrorMessage(
        emptyFieldKey1 = s"pensions.pensionAmount.totalTax.error.noEntry.$agentIndividual",
        wrongFormatKey1 = s"pensions.pensionAmount.totalTax.error.incorrectFormat",
        exceedsMaxAmountKey1 = s"pensions.pensionAmount.totalTax.error.overMaximum",
        emptyFieldKey2 = s"pensions.pensionAmount.taxPaid.error.noEntry.$agentIndividual",
        wrongFormatKey2 = s"pensions.pensionAmount.taxPaid.error.incorrectFormat.$agentIndividual",
        exceedsMaxAmountKey2 = s"pensions.pensionAmount.taxPaid.error.overMaximum"
      ))
  }

  def pensionPaymentsForm(user: User): Form[(Option[BigDecimal], Option[BigDecimal])] =
    OptionalTupleAmountForm.amountForm(
      OptionalTupleAmountFormErrorMessage(
        emptyFieldKey1 = "overseasPension.pensionPayments.amountBeforeTax.noEntry",
        wrongFormatKey1 = s"overseasPension.pensionPayments.amountBeforeTax.incorrectFormat.${userType(user.isAgent)}",
        exceedsMaxAmountKey1 = "overseasPension.pensionPayments.amountBeforeTax.tooBig",
        emptyFieldKey2 = "common.pensions.error.amount.noEntry",
        wrongFormatKey2 = "overseasPension.pensionPayments.nonUkTaxPaid.incorrectFormat",
        exceedsMaxAmountKey2 = "common.pensions.error.amount.overMaximum",
        taxPaidLessThanAmountBeforeTaxErrorMessage = "overseasPension.pensionPayments.nonUkTaxPaidLessThanAmountBeforeTax"
      ))

  def statePensionForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.statePension.error.noEntry.$agentOrIndividual",
      emptyFieldKey = "pensions.statePension.amount.error.incorrectOrEmpty",
      wrongFormatKey = "pensions.statePension.amount.error.incorrectOrEmpty",
      exceedsMaxAmountKey = s"pensions.statePension.amount.error.overMaximum.$agentOrIndividual"
    )
  }

  def untaxedEmployerPayments(isAgent: Boolean): Form[BigDecimal] = {
    val agentOrIndividual = userType(isAgent)
    AmountForm.amountForm(
      emptyFieldKey = s"overseasPension.untaxedEmployerPayments.error.noEntry.$agentOrIndividual",
      wrongFormatKey = s"overseasPension.untaxedEmployerPayments.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"common.overseas.pension.schemes.error.tooBig.$agentOrIndividual"
    )
  }

  def sf74ReferenceIdForm: Form[String] = SF74ReferenceForm.sf74ReferenceIdForm(
    noEntryMsg = "pensions.paymentsIntoOverseasPensions.sf74Reference.noEntry",
    incorrectFormatMsg = "pensions.paymentsIntoOverseasPensions.sf74Reference.incorrectFormat"
  )

  def overseasPensionsReliefTypeForm(user: User): Form[String] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonForm.radioButtonForm(s"overseasPension.pensionReliefType.error.noEntry.$agentOrIndividual", TaxReliefQuestion.validTaxList)
  }

  def taxPaidOnStatePensionLumpSum(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.taxPaidOnStatePensionLumpSum.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"pensions.taxPaidOnStatePensionLumpSum.amount.error.noEntry.$agentOrIndividual",
      wrongFormatKey = "pensions.taxPaidOnStatePensionLumpSum.amount.error.incorrectFormat",
      exceedsMaxAmountKey = s"pensions.taxPaidOnStatePensionLumpSum.amount.error.overMaximum.$agentOrIndividual"
    )
  }

  def statePensionLumpSum(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"incomeFromPensions.statePensionLumpSum.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"incomeFromPensions.statePensionLumpSum.amount.error.noEntry.$agentOrIndividual",
      wrongFormatKey = s"incomeFromPensions.statePensionLumpSum.amount.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"incomeFromPensions.statePensionLumpSum.amount.error.overMaximum.$agentOrIndividual"
    )
  }

  def aboveAnnualAllowanceForm(user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(user.isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.aboveReducedAnnualAllowance.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"pensions.aboveReducedAnnualAllowance.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"pensions.aboveReducedAnnualAllowance.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"pensions.aboveReducedAnnualAllowance.error.overMaximum.$agentOrIndividual"
    )
  }

  def stateBenefitDateForm: Form[DateForm.DateModel] =
    DateForm.dateForm("stateBenefitStartDate")

  def statePensionLumpSumStartDateForm: Form[DateForm.DateModel] =
    DateForm.dateForm("statePensionLumpSumStartDate")

  def pensionSchemeDateForm: Form[DateForm.DateModel] =
    DateForm.dateForm("pensionStartDate")

  def unauthorisedNonUkTaxOnSurchargedAmountForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"unauthorisedPayments.didYouPayNonUkTax.error.noEntry.$agentOrIndividual",
      emptyFieldKey = "common.pensions.error.amount.noEntry",
      wrongFormatKey = "common.unauthorisedPayments.error.Amount.incorrectFormat",
      exceedsMaxAmountKey = "common.pensions.error.amount.overMaximum"
    )
  }

  def unauthorisedNonUkTaxOnNotSurchargedAmountForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"unauthorisedPayments.nonUkTaxOnAmountNotSurcharge.error.noEntry.$agentOrIndividual",
      emptyFieldKey = "common.pensions.error.amount.noEntry",
      wrongFormatKey = "common.unauthorisedPayments.error.Amount.incorrectFormat",
      exceedsMaxAmountKey = "common.pensions.error.amount.overMaximum"
    )
  }
}

object FormsProvider {
  val userType: Boolean => String = (isAgent: Boolean) => if (isAgent) "agent" else "individual"

  def pensionProviderPaidTaxForm(isAgent: Boolean): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = userType(isAgent)
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"pensions.pensionsProviderPaidTax.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"pensions.pensionsProviderPaidTax.error.noAmount.$agentOrIndividual",
      wrongFormatKey = s"pensions.pensionsProviderPaidTax.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"common.pensions.error.amountMaxLimit.$agentOrIndividual",
      minAmountKey = "common.error.amountNotZero"
    )
  }
}
