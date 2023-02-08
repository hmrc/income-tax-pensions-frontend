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

package controllers.pensions.paymentsIntoPensions

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class PaymentsIntoPensionFormProvider {
  def reliefAtSourcePensionsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.reliefAtSource.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def reliefAtSourcePaymentsAndTaxReliefAmountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.noEntry",
    wrongFormatKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.invalidFormat",
    exceedsMaxAmountKey = "pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.overMaximum"
  )

  def oneOffRASPaymentsAmountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "paymentsIntoPensions.oneOffRasAmount.error.noEntry",
    wrongFormatKey = "paymentsIntoPensions.oneOffRasAmount.error.invalidFormat",
    exceedsMaxAmountKey = "paymentsIntoPensions.oneOffRasAmount.error.overMaximum"
  )

  def totalPaymentsIntoRASForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "paymentsIntoPensions.totalRASPayments.error"
  )

  def pensionsTaxReliefNotClaimedForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.pensionsTaxReliefNotClaimed.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def retirementAnnuityForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.retirementAnnuityContract.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def retirementAnnuityAmountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.retirementAnnuityAmount.error.noEntry",
    wrongFormatKey = "pensions.retirementAnnuityAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.retirementAnnuityAmount.error.overMaximum"
  )

  def workplacePensionForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.workplacePension.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def workplacePensionAmountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.workplaceAmount.error.noEntry",
    wrongFormatKey = "pensions.workplaceAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.workplaceAmount.error.maxAmount"
  )

  def reliefAtSourceOneOffPaymentsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.reliefAtSourceOneOffPayments.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def paymentsIntoPensionsStatusForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"paymentsIntoPensions.statusPage.error.${if (isAgent) "agent" else "individual"}"
  )

}

