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

import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel}

object PensionAnnualAllowanceViewModelBuilder {

  val aPensionAnnualAllowanceViewModel: PensionAnnualAllowancesViewModel = PensionAnnualAllowancesViewModel(
    reducedAnnualAllowanceQuestion = Some(true),
    moneyPurchaseAnnualAllowance = Some(true),
    taperedAnnualAllowance = Some(true),
    aboveAnnualAllowanceQuestion = Some(true),
    aboveAnnualAllowance = Some(12.44),
    pensionProvidePaidAnnualAllowanceQuestion = Some("Yes"),
    taxPaidByPensionProvider = Some(14.55),
    pensionSchemeTaxReference = Some(Seq("1234567CRC","12345678RB","1234567DRD"))
  )

  val aPensionAnnualAllowanceEmptyViewModel: PensionAnnualAllowancesViewModel = PensionAnnualAllowancesViewModel()

}
