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

import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionReliefsBuilder.anPensionReliefs
import builders.StateBenefitsModelBuilder.aStateBenefitsModel
import builders.EmploymentPensionsBuilder.anEmploymentPensions
import builders.PensionIncomeViewModelBuilder.aPensionIncome
import models.pension.AllPensionsData

object AllPensionsDataBuilder {

  val anAllPensionsData: AllPensionsData = AllPensionsData(
    pensionReliefs = Some(anPensionReliefs),
    pensionCharges = Some(anPensionCharges),
    stateBenefits = Some(aStateBenefitsModel),
    employmentPensions = Some(anEmploymentPensions),
    pensionIncome = Some(aPensionIncome)
  )

  val anAllPensionDataEmpty: AllPensionsData = AllPensionsData(
    pensionReliefs = None,
    pensionCharges = None,
    stateBenefits = None,
    employmentPensions = None,
    pensionIncome = None
  )
}
