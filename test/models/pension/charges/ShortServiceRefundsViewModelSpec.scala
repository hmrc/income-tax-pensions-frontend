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

import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, emptyShortServiceRefundsViewModel}
import support.UnitTest

class ShortServiceRefundsViewModelSpec extends UnitTest {

  ".isEmpty" should {
    "return true when no questions have been answered" in {
      emptyShortServiceRefundsViewModel.isEmpty
    }
    "return false when any questions have been answered" in {
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(true)).isEmpty
      aShortServiceRefundsViewModel.isEmpty
    }
  }

  ".isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aShortServiceRefundsViewModel.isFinished
      }
      "all required questions are answered" in {
        emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false)).isFinished
        emptyShortServiceRefundsViewModel.copy(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(25),
          shortServiceRefundTaxPaid = Some(false)
        ).isFinished
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aShortServiceRefundsViewModel.copy(shortServiceRefundTaxPaidCharge = None).isFinished
        emptyShortServiceRefundsViewModel.copy(
          shortServiceRefund = Some(true), shortServiceRefundTaxPaid = Some(false)
        ).isFinished
      }
    }
  }

  ".journeyIsNo" should {
    "return true when shortServiceRefund is 'false' and no others have been answered" in {
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      emptyShortServiceRefundsViewModel.journeyIsNo
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(true)).journeyIsNo
      aShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false)).journeyIsNo
    }
  }

  ".toOverseasPensionContributions" should {
    "transform a ShortServiceRefundsViewModel into a OverseasPensionContributions" in {
      val result = OverseasPensionContributions(overseasSchemeProvider = Seq(
        OverseasSchemeProvider(
          providerName = "Overseas Refund Scheme Name",
          providerAddress = "Scheme Address",
          providerCountryCode = "FRA",
          qualifyingRecognisedOverseasPensionScheme = Some(Seq("QOPS123456")),
          pensionSchemeTaxReference = None
        )),
        shortServiceRefund = 1999.99,
        shortServiceRefundTaxPaid = 1000.00
      )
      aShortServiceRefundsViewModel.toOverseasPensionContributions shouldBe result
    }
  }
}
