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

import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}

object PensionLifetimeAllowanceViewModelBuilder {
  val aPensionLifetimeAllowanceViewModel: PensionLifetimeAllowancesViewModel = PensionLifetimeAllowancesViewModel(
    aboveLifetimeAllowanceQuestion = Some(true),
    pensionAsLumpSum = Some(LifetimeAllowance(134.22, 23.55)),
    pensionPaidAnotherWayQuestion = Some(true),
    pensionPaidAnotherWay = Some(LifetimeAllowance(1667.22, 11.33)),
    pensionAsLumpSumQuestion = Some(true)
  )

  val aPensionLifetimeAllowancesEmptyViewModel: PensionLifetimeAllowancesViewModel = PensionLifetimeAllowancesViewModel()

}
