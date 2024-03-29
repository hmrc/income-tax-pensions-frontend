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

import models.pension.charges.PensionContributions

object PensionContributionsBuilder {

  val anPensionContributions: PensionContributions = PensionContributions(
    pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
    inExcessOfTheAnnualAllowance = 321.71,
    annualAllowanceTaxPaid = 888.67,
    isAnnualAllowanceReduced = Some(true),
    taperedAnnualAllowance = Some(true),
    moneyPurchasedAllowance = Some(true)
  )
}
