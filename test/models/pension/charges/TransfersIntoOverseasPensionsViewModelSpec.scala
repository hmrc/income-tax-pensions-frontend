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

import builders.TransfersIntoOverseasPensionsViewModelBuilder._
import support.UnitTest

class TransfersIntoOverseasPensionsViewModelSpec extends UnitTest {

  ".isEmpty" should {
    "return true when no questions have been answered" in {
      emptyTransfersIntoOverseasPensionsViewModel.isEmpty
    }
    "return false when any questions have been answered" in {
      emptyTransfersIntoOverseasPensionsViewModel.copy(transferPensionSavings = Some(true)).isEmpty shouldBe false
      aTransfersIntoOverseasPensionsViewModel.isEmpty shouldBe false
    }
  }

  ".isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aTransfersIntoOverseasPensionsViewModel.isFinished
      }
      "all required questions are answered" in {
        aTransfersIntoOverseasPensionsViewModel
          .copy(
            pensionSchemeTransferCharge = Some(false),
            pensionSchemeTransferChargeAmount = None,
            transferPensionScheme = Seq.empty
          )
          .isFinished
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        !aTransfersIntoOverseasPensionsViewModel.copy(pensionSchemeTransferChargeAmount = None).isFinished
      }
    }
  }
}
