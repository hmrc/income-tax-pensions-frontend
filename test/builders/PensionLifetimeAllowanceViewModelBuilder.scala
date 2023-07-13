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

import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}

object PensionLifetimeAllowanceViewModelBuilder {
  val aPensionLifetimeAllowanceViewModel: PensionLifetimeAllowancesViewModel = PensionLifetimeAllowancesViewModel(
    aboveLifetimeAllowanceQuestion = Some(true),
    pensionAsLumpSumQuestion = Some(true),
    pensionAsLumpSum = Some(LifetimeAllowance(Some(134.22), Some(23.55))),
    pensionPaidAnotherWayQuestion = Some(true),
    pensionPaidAnotherWay = Some(LifetimeAllowance(Some(1667.22), Some(11.33))),
    pensionSchemeTaxReferences = Some(Seq("1234567CRC", "12345678RB", "1234567DRD"))
  )

  val minimalPensionLifetimeAllowanceViewModel: PensionLifetimeAllowancesViewModel = PensionLifetimeAllowancesViewModel(
    aboveLifetimeAllowanceQuestion = Some(false)
  )

  val aPensionLifetimeAllowancesEmptyViewModel: PensionLifetimeAllowancesViewModel = PensionLifetimeAllowancesViewModel()

}
