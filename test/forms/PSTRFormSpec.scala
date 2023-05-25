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

    "provided a correct PSTR that starts with 8 numbers and 1 upper case 'R' and 1 upper case letter" should {
      val pstr = "12345678RB"
      val pstr2 = "45671238RG"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
      }
    }

    "provided a correct PSTR that starts with 8 numbers and 1 upper case 'R' and 1 lower case letter" should {
      val pstr = "12345678Rc"
      val pstr2 = "12567348Rc"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
      }
    }

    "provided an incorrect PSTR that does not contain an upper case 'R' in the 9th position" should {
      val pstr = "12345781c"
      val pstr2 = "1234578ac"
      val pstr3 = "1257348rZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
        checkRegex(pstr3) shouldBe false
      }
    }

    "provided an incorrect PSTR that starts with fewer than 8 numbers" should {
      val pstr = "1234578Rc"
      val pstr2 = "1257348RZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that starts with more than 8 numbers" should {
      val pstr = "1234567899Rc"
      val pstr2 = "1257348899RZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that contains a symbol" should {
      val pstr = "1234578R#"
      val pstr2 = "1257348+R"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that contains only letters" should {
      val pstr = "abcdefghRi"
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