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

package common

object MessageKeys {
  object LifetimeAllowance {
    object PensionProviderPaidTax extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.selectYesifYourPensionProvider.noEntry.individual",
          "common.pensions.selectYesifYourPensionProvider.noEntry.agent")
      override val amountEmpty: UserTypeMessage =
        UserTypeMessage(
          "pensions.pensionsProviderPaidTax.error.noAmount.individual",
          "pensions.pensionsProviderPaidTax.error.noAmount.agent")
      override val minAmountMessage: UserTypeMessage = UserTypeMessage("","")
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "pensions.pensionsProviderPaidTax.error.incorrectFormat.individual",
          "pensions.pensionsProviderPaidTax.error.incorrectFormat.agent")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amountMaxLimit.individual",
          "common.pensions.error.amountMaxLimit.agent")
    }
  }
  object OverseasPensions {
    object PaymentIntoScheme extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.paymentIntoOverseasPensionScheme.radio.error.individual",
          "overseasPension.paymentIntoOverseasPensionScheme.radio.error.agent")
      override val amountEmpty: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.paymentIntoOverseasPensionScheme.no.entry.error.individual",
          "overseasPension.paymentIntoOverseasPensionScheme.no.entry.error.agent")
      override val minAmountMessage: UserTypeMessage = UserTypeMessage("","")
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.individual",
          "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.agent")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "common.overseas.pension.schemes.error.tooBig.individual",
          "common.overseas.pension.schemes.error.tooBig.agent")
    }

  }

  object IncomeFromOverseasPensions {
    object ForeignTaxCreditRelief extends YesNoForm {
      override val noEntry: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.foreignTaxCreditRelief.error.noEntry.individual",
          "overseasPension.foreignTaxCreditRelief.error.noEntry.agent"
        )
    }
  }

  object UnauthorisedPayments {
    object NonUKTaxOnAmountResultedInSurcharge extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage =
        UserTypeMessage(
          "unauthorisedPayments.didYouPayNonUkTax.error.noEntry",
          "unauthorisedPayments.didYouPayNonUkTax.error.noEntry")
      override val amountEmpty: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amount.noEntry",
          "common.pensions.error.amount.noEntry")
      override val minAmountMessage: UserTypeMessage = UserTypeMessage("","")
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "common.unauthorisedPayments.error.Amount.incorrectFormat",
          "common.unauthorisedPayments.error.Amount.incorrectFormat")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amount.overMaximum",
          "common.pensions.error.amount.overMaximum")
    }
    object NonUKTaxOnAmountNotResultedInSurcharge extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage =
        UserTypeMessage(
          "unauthorisedPayments.nonUkTaxOnAmountNotSurcharge.error.noEntry",
          "unauthorisedPayments.nonUkTaxOnAmountNotSurcharge.error.noEntry")
      override val amountEmpty: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amount.noEntry",
          "common.pensions.error.amount.noEntry")
      override val minAmountMessage: UserTypeMessage = UserTypeMessage("","")
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "common.unauthorisedPayments.error.Amount.incorrectFormat",
          "common.unauthorisedPayments.error.Amount.incorrectFormat")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amount.overMaximum",
          "common.pensions.error.amount.overMaximum")
    }
    object SpecialWithholdingTax extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.specialWithHoldingTax.amount.individual.noEntry",
          "overseasPension.specialWithHoldingTax.amount.agent.noEntry")
      override val amountEmpty: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.specialWithHoldingTax.amount.noAmountEntry",
          "overseasPension.specialWithHoldingTax.amount.noAmountEntry")
      override val minAmountMessage: UserTypeMessage = UserTypeMessage(
        "common.error.amountNotZero",
        "common.error.amountNotZero")
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.specialWithHoldingTax.amount.incorrectFormat",
          "overseasPension.specialWithHoldingTax.amount.incorrectFormat")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.specialWithHoldingTax.amount.tooBig",
          "overseasPension.specialWithHoldingTax.amount.tooBig")
    }
  }

  sealed case class UserTypeMessage(individual: String, agent: String) {
    def get(isAgent: Boolean): String = if (isAgent) agent else individual
  }

  trait YesNoAmountForm {
    val neitherYesNorNo: UserTypeMessage
    val amountEmpty: UserTypeMessage
    val amountHasInvalidFormat: UserTypeMessage
    val minAmountMessage: UserTypeMessage
    val amountIsExcessive: UserTypeMessage
  }

  trait YesNoForm {
    val noEntry: UserTypeMessage
  }
}

