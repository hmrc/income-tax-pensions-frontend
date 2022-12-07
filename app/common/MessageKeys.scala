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
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "pensions.pensionProviderPaidTax.error.incorrectFormat.individual",
          "pensions.pensionProviderPaidTax.error.incorrectFormat.agent")
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
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.individual",
          "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.agent")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "overseasPension.paymentIntoOverseasPensionScheme.maximum.error.individual",
          "overseasPension.paymentIntoOverseasPensionScheme.maximum.error.agent")
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
      override val amountHasInvalidFormat: UserTypeMessage =
        UserTypeMessage(
          "common.unauthorisedPayments.error.Amount.incorrectFormat",
          "common.unauthorisedPayments.error.Amount.incorrectFormat")
      override val amountIsExcessive: UserTypeMessage =
        UserTypeMessage(
          "common.pensions.error.amount.overMaximum",
          "common.pensions.error.amount.overMaximum")
    }
  }

  sealed case class UserTypeMessage(individual: String, agent: String) {
    def get(isAgent: Boolean): String = if (isAgent) agent else individual
  }

  trait YesNoAmountForm {
    val neitherYesNorNo: UserTypeMessage
    val amountEmpty: UserTypeMessage
    val amountHasInvalidFormat: UserTypeMessage
    val amountIsExcessive: UserTypeMessage
  }

  trait YesNoForm {
    val noEntry: UserTypeMessage
  }
}

