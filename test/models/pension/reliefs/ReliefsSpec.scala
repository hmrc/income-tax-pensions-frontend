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

package models.pension.reliefs

import builders.ReliefsBuilder.{aReliefs, anEmptyReliefs}
import utils.UnitTest

class ReliefsSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      anEmptyReliefs.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aReliefs.isEmpty shouldBe false
      aReliefs.copy(regularPensionContributions = None).isEmpty shouldBe false
      anEmptyReliefs.copy(regularPensionContributions = Some(100.00)).isEmpty shouldBe false
    }
  }

}
