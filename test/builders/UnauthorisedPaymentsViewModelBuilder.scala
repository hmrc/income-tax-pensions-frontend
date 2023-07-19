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

package builders

import models.pension.charges.UnauthorisedPaymentsViewModel

object UnauthorisedPaymentsViewModelBuilder {

  val anUnauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel(
    surchargeQuestion = Some(true),
    noSurchargeQuestion = Some(true),
    surchargeAmount = Some(12.11),
    surchargeTaxAmountQuestion = Some(true),
    surchargeTaxAmount = Some(34.22),
    noSurchargeAmount = Some(88.11),
    noSurchargeTaxAmountQuestion = Some(true),
    noSurchargeTaxAmount = Some(99.22),
    ukPensionSchemesQuestion = Some(true),
    pensionSchemeTaxReference = Some(Seq("12345678AB", "12345678AC"))
  )

  val anUnauthorisedPaymentsEmptySchemesViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel(
    surchargeQuestion = Some(true),
    noSurchargeQuestion = Some(true),
    surchargeAmount = Some(12.11),
    surchargeTaxAmountQuestion = Some(true),
    surchargeTaxAmount = Some(34.22),
    noSurchargeAmount = Some(88.11),
    noSurchargeTaxAmountQuestion = Some(true),
    noSurchargeTaxAmount = Some(99.22),
    ukPensionSchemesQuestion = Some(false),
    pensionSchemeTaxReference = None
  )

  val anUnauthorisedPaymentsEmptyViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel()

}
