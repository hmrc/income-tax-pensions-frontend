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

import builders.PensionReliefsBuilder.anPensionReliefs
import utils.UnitTest

class CreateUpdatePensionReliefsRequestModelSpec extends UnitTest {

  ".otherSubModelsEmpty" should {

    Seq(
      anPensionReliefs.pensionReliefs
    ).foreach { subModel =>
      s"be false when it is the only sub model in model request" in {
        val actualResult = CreateUpdatePensionReliefsModel(
          anPensionReliefs.pensionReliefs
        ).otherSubRequestModelsEmpty(Some(subModel))
        actualResult shouldBe true
      }
    }
  }
}
