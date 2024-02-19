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
