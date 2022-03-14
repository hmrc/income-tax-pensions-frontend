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

package builders

import models.pension.reliefs.PaymentsIntoPensionViewModel

object PaymentsIntoPensionVewModelBuilder {

  val aPaymentsIntoPensionViewModel: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true),
    totalRASPaymentsAndTaxRelief = Some(189.01),
    oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
    totalOneOffRasPaymentPlusTaxRelief = Some(190.01),
    pensionTaxReliefNotClaimedQuestion = Some(true),
    retirementAnnuityContractPaymentsQuestion = Some(true),
    totalRetirementAnnuityContractPayments = Some(191.01),
    workplacePensionPaymentsQuestion = Some(true),
    totalWorkplacePensionPayments = Some(192.01)
  )

  val aPaymentsIntoPensionsEmptyViewModel: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel()
}