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

import models.User
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class FormsProvider() {
  def overseasTransferChargePaidForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "transferIntoOverseasPensions.overseasTransferChargesPaid.error.noEntry"
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
      emptyFieldKey = s"shortServiceRefunds.taxableRefundAmount.error.noAmountEntry",
      wrongFormatKey = s"shortServiceRefunds.taxableRefundAmount.error.incorrectFormat",
      exceedsMaxAmountKey = s"shortServiceRefunds.taxableRefundAmount.error.tooBig"
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
}
