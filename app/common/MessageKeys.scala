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
  object OverseasPensions {
    object PaymentIntoScheme extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage = new UserTypeMessage {
        private val individual = "overseasPension.paymentIntoOverseasPensionScheme.radio.error.individual"
        private val agent = "overseasPension.paymentIntoOverseasPensionScheme.radio.error.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountEmpty: UserTypeMessage = new UserTypeMessage {
        private val individual = "overseasPension.paymentIntoOverseasPensionScheme.no.entry.error.individual"
        private val agent = "overseasPension.paymentIntoOverseasPensionScheme.no.entry.error.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountHasInvalidFormat: UserTypeMessage = new UserTypeMessage {
        private val individual = "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.individual"
        private val agent = "overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountIsExcessive: UserTypeMessage = new UserTypeMessage {
        private val individual = "overseasPension.paymentIntoOverseasPensionScheme.maximum.error.individual"
        private val agent = "overseasPension.paymentIntoOverseasPensionScheme.maximum.error.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
    }
  }
  object LifetimeAllowance {
    object PensionProviderPaidTax extends YesNoAmountForm {
      override val neitherYesNorNo: UserTypeMessage = new  UserTypeMessage {
        private val individual = "common.pensions.selectYesifYourPensionProvider.noEntry.individual"
        private val agent = "common.pensions.selectYesifYourPensionProvider.noEntry.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountEmpty: UserTypeMessage = new UserTypeMessage {
        private val individual = "pensions.pensionsProviderPaidTax.error.noAmount.individual"
        private val agent = "pensions.pensionsProviderPaidTax.error.noAmount.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountHasInvalidFormat: UserTypeMessage = new UserTypeMessage {
        private val individual = "pensions.pensionProviderPaidTax.error.incorrectFormat.individual"
        private val agent = "pensions.pensionProviderPaidTax.error.incorrectFormat.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
      override val amountIsExcessive: UserTypeMessage = new UserTypeMessage {
        private val individual = "common.pensions.error.amountMaxLimit.individual"
        private val agent = "common.pensions.error.amountMaxLimit.agent"
        def get(isAgent: Boolean): String = if (isAgent) agent else individual
      }
    }
  }

  trait UserTypeMessage {
    def get(isAgent: Boolean): String
  }

  trait YesNoAmountForm {
    val neitherYesNorNo: UserTypeMessage
    val amountEmpty: UserTypeMessage
    val amountHasInvalidFormat: UserTypeMessage
    val amountIsExcessive: UserTypeMessage
  }

}

