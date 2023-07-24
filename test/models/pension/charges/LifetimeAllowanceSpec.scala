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

package models.pension.charges

import builders.LifetimeAllowanceBuilder.{aLifetimeAllowance1, anEmptyLifetimeAllowance}
import support.UnitTest

class LifetimeAllowanceSpec extends UnitTest {

  "isFinished" should {
    "return true when all parameters are populated" in {
        aLifetimeAllowance1.isFinished
    }
    "return false when any parameters are incomplete" in {
      aLifetimeAllowance1.copy(amount = None).isFinished shouldBe false
      anEmptyLifetimeAllowance.copy(amount = Some(22.22)).isFinished shouldBe false
      }
    }

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      anEmptyLifetimeAllowance.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aLifetimeAllowance1.copy(amount = None).isFinished shouldBe false
      anEmptyLifetimeAllowance.copy(amount = Some(22.22)).isFinished shouldBe false
    }
  }

}
