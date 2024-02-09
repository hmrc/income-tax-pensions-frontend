package utils

import org.scalatest.wordspec.AnyWordSpecLike
import utils.EqualsHelper.isDifferent

class EqualsHelperSpec extends AnyWordSpecLike {
  final case class Model(a: Option[Boolean], b: Option[BigDecimal])

  "isDifferent" should {
    "return false for None on the right hand side" in {
      val left = Model(None, None)
      assert(isDifferent(left, None) === false)
    }

    "return false for equal objects" in {
      val left  = Model(Some(true), Some(BigDecimal("123.0")))
      val right = Some(Model(Some(true), Some(BigDecimal("130.0"))))
      assert(isDifferent(left, right) === false)
    }

    "return true for equal objects" in {
      val left  = Model(Some(true), Some(BigDecimal("123.0")))
      val right = Some(Model(Some(true), Some(BigDecimal("123.0"))))
      assert(isDifferent(left, right) === true)
    }

  }
}
