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

    "provided a correct PSTR that starts with 8 numbers then one 'R' then one other letter" should {
      val pstr  = "12345678RB"
      val pstr2 = "45671238RG"
      val pstr3 = "12567348Rc"
      val pstr4 = "12567348rc"
      val pstr5 = "12567348rX"

      "return true" in {
        checkRegex(pstr) shouldBe true
        checkRegex(pstr2) shouldBe true
        checkRegex(pstr3) shouldBe true
        checkRegex(pstr4) shouldBe true
        checkRegex(pstr5) shouldBe true
      }
    }

    "provided an incorrect PSTR that does not contain an 'R' in the 9th position" should {
      val pstr  = "12345781c"
      val pstr2 = "1234578ac"
      val pstr3 = "125734RdR"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
        checkRegex(pstr3) shouldBe false
      }
    }

    "provided an incorrect PSTR that contains too few characters" should {
      val pstr  = "1234567Rr"
      val pstr2 = "12345678R"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that starts with more than 8 numbers" should {
      val pstr  = "1234567899Rc"
      val pstr2 = "1257348R9RRZ"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that contains a symbol" should {
      val pstr  = "1234578R#"
      val pstr2 = "1234567+Rc"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
      }
    }

    "provided an incorrect PSTR that does not follow the correct format" should {
      val pstr  = "abcdefghRi"
      val pstr2 = "123456R8RR"
      val pstr3 = "12345678AR"
      val pstr4 = "12345678R8"

      "return false" in {
        checkRegex(pstr) shouldBe false
        checkRegex(pstr2) shouldBe false
        checkRegex(pstr3) shouldBe false
        checkRegex(pstr4) shouldBe false
      }
    }

  }

  private def checkRegex(input: String) =
    PensionSchemeTaxReferenceForm.filterAndUpperCase(input).matches(PensionSchemeTaxReferenceForm.regex)
}
