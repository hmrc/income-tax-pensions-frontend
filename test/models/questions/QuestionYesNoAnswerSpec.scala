package models.questions

import org.scalatest.wordspec.AnyWordSpecLike
import QuestionYesNoAnswer._

class QuestionYesNoAnswerSpec extends AnyWordSpecLike {
  "QuestionYesNoAnswer" should {
    "create Yes for non 0 Big Decimal" in {
      assert(QuestionYesNoAnswer(Some(10.0)) === Yes(BigDecimal("10.0")))
    }

    "create NotDefined for 0.0" in {
      assert(QuestionYesNoAnswer(Some(0.0)) === No)
    }

    "created No for Not defined answer" in {
      assert(QuestionYesNoAnswer(None) === NotDefined)
    }
  }
}
