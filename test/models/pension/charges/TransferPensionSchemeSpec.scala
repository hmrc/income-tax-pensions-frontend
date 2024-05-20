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

import builders.TransferPensionSchemeBuilder._
import utils.UnitTest

class TransferPensionSchemeSpec extends UnitTest {

  "isFinished" should {
    "return true when all questions are populated" in {
      aUkTransferPensionScheme.isFinished shouldBe true
      aNonUkTransferPensionScheme.isFinished shouldBe true
    }
    "return false when not all necessary questions have been populated" in {
      anEmptyTransferPensionScheme.isFinished shouldBe false
      aNonUkTransferPensionScheme.copy(alphaThreeCountryCode = None).isFinished shouldBe false
    }
  }

}
