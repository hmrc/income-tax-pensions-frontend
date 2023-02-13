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

package forms

import utils.UnitTest

class PSTRFormSpec extends UnitTest {

  "The Pension Scheme Tax Reference Form regex" when {

    "provided a correct PSTR that starts with 8 numbers and 2 upper case letters" should {
      val pstr = "12345678AB"
      val pstr2 = "45671238CG"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
      }
    }

    "provided a correct PSTR that starts with 8 numbers and 2 lower case letters" should {
      val pstr = "12345678zc"
      val pstr2 = "12567348cc"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
      }
    }

    "provided a correct PSTR that starts with 8 numbers, 1 upper case and 1 lower case letter" should {
      val pstr = "12345678Ac"
      val pstr2 = "12567348cZ"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
      }
    }

    "provided an incorrect PSTR that starts with 7 numbers, 1 upper case and 1 lower case letter" should {
      val pstr = "1234578Ac"
      val pstr2 = "1257348cZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that starts with 10 numbers, 1 upper case and 1 lower case letter" should {
      val pstr = "1234567899Ac"
      val pstr2 = "1257348899cZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that starts with 8 numbers, a letter and a symbol" should {
      val pstr = "1234578A#"
      val pstr2 = "1257348+Z"
      val pstr3 = "1257348m"


      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
        checkRegex(pstr3) shouldBe false
      }
    }

    "provided an incorrect PSTR that contains only letter" should {
      val pstr = "abcdefghii"
      val pstr2 = "adefgbchii"


      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

  }

  private def checkRegex(input: String) = {
    input.matches(PensionSchemeTaxReferenceForm.regex)
  }
}