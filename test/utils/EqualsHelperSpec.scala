/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import org.scalatest.wordspec.AnyWordSpecLike
import utils.EqualsHelper.isDifferent

class EqualsHelperSpec extends AnyWordSpecLike {
  final case class Model(a: Option[Boolean], b: Option[BigDecimal])

  "isDifferent" should {
    "return true for None on the right hand side" in {
      val left = Model(None, None)
      assert(isDifferent(left, None) === true)
    }

    "return true for different objects" in {
      val left  = Model(Some(true), Some(BigDecimal("123.0")))
      val right = Some(Model(Some(true), Some(BigDecimal("130.0"))))
      assert(isDifferent(left, right) === true)
    }

    "return true for equal objects" in {
      val left  = Model(Some(true), Some(BigDecimal("123.0")))
      val right = Some(Model(Some(true), Some(BigDecimal("123.0"))))
      assert(isDifferent(left, right) === false)
    }

  }
}
