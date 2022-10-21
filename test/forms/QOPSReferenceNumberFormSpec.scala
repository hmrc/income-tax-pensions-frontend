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

package forms

import utils.UnitTest

class QOPSReferenceNumberFormSpec extends UnitTest {
  "The QOPS Reference Number Form regex" when {

    "provided a correct QOPS that contains 6 numbers" should {
      val qops = "123456"
      val qops2 = "435627"

      "return true" in {
        checkRegex(qops) shouldBe true
        checkRegex(qops2) shouldBe true
      }
    }

    "provided a correct QOPS that contains an empty string" should {
      val qops = ""

      "return true" in {
        checkRegex(qops) shouldBe true
      }
    }

    "provided a incorrect QOPS that contains 7 numbers" should {
      val qops = "1234567"
      val qops2 = "4356287"

      "return false" in {
        checkRegex(qops) shouldBe false
        checkRegex(qops2) shouldBe false
      }
    }

    "provided a incorrect QOPS that contains 6 letters" should {
      val qops = "abcdef"
      val qops2 = "nmvdks"

      "return false" in {
        checkRegex(qops) shouldBe false
        checkRegex(qops2) shouldBe false
      }
    }
//CASE where QOPS contains padding
  }

  private def checkRegex(input: String) = {
    input.matches(QOPSReferenceNumberForm.regex)
  }
}
