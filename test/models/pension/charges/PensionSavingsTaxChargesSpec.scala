package models.pension.charges

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import cats.implicits.catsSyntaxOptionId
import models.IncomeTaxUserData
import utils.UnitTest

class PensionSavingsTaxChargesSpec extends UnitTest {

  val noPriorData: IncomeTaxUserData =
    IncomeTaxUserData(None)

  val someEmptyPriorData: IncomeTaxUserData =
    IncomeTaxUserData(anAllPensionDataEmpty.some)

  val someFilledPriorData: IncomeTaxUserData =
    IncomeTaxUserData(anAllPensionsData.some)

  val emptyPstc: PensionSavingsTaxCharges =
    PensionSavingsTaxCharges(None, None, None)

  "Generating PensionSavingsTaxCharges model from prior data" when {
    "there is no pensions prior data" should {
      "return None" in {
        PensionSavingsTaxCharges.fromPriorData(noPriorData) shouldBe None
      }
    }
    "there is pensions prior data" when {
      "each field in the PensionSavingsTaxCharges is None" should {
        "return None" in {
          PensionSavingsTaxCharges.fromPriorData(someEmptyPriorData) shouldBe None
        }
      }
      "at least one field in the PensionSavingsTaxCharges is present" should {
        "return the model" in {
          val expectedResult =
            PensionSavingsTaxCharges(
              Some(List("00123456RA", "00123456RB")),
              Some(LifetimeAllowance(Some(22.22), Some(11.11))),
              Some(LifetimeAllowance(Some(22.22), Some(11.11))))

          PensionSavingsTaxCharges.fromPriorData(someFilledPriorData) shouldBe expectedResult.some
        }
      }
    }
  }

}
