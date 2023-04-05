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

import builders.StateBenefitBuilder._
import models.pension.statebenefits.StateBenefitsData

object StateBenefitsBuilder {

  val anStateBenefts: StateBenefitsData = StateBenefitsData(
    incapacityBenefits = Some(Set(anStateBenefitOne, anStateBenefitTwo)),
    statePension = Some(anStateBenefitThree),
    statePensionLumpSum = Some(anStateBenefitFour),
    employmentSupportAllowances = Some(Set(anStateBenefitFive, anStateBenefitSix)),
    jobSeekersAllowances = Some(Set(anStateBenefitSeven, anStateBenefitEight)),
    bereavementAllowance = Some(anStateBenefitNine),
    other = Some(anStateBenefitTen)
  )
}
