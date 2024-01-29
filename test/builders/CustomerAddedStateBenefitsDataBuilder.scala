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

import builders.CustomerAddedStateBenefitBuilder._
import models.pension.statebenefits.CustomerAddedStateBenefitsData

object CustomerAddedStateBenefitsDataBuilder {

  val aCustomerAddedStateBenefits: CustomerAddedStateBenefitsData = CustomerAddedStateBenefitsData(
    incapacityBenefits = Some(Set(aCustomerAddedStateBenefitOne, aCustomerAddedStateBenefitTwo)),
    statePensions = Some(Set(aCustomerAddedStateBenefitThree)),
    statePensionLumpSums = Some(Set(aCustomerAddedStateBenefitFour)),
    employmentSupportAllowances = Some(Set(aCustomerAddedStateBenefitFive, aCustomerAddedStateBenefitSix)),
    jobSeekersAllowances = Some(Set(aCustomerAddedStateBenefitSeven, aCustomerAddedStateBenefitEight)),
    bereavementAllowances = Some(Set(aCustomerAddedStateBenefitNine)),
    otherStateBenefits = Some(Set(aCustomerAddedStateBenefitTen))
  )
}
