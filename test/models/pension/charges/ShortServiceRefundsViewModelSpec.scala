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

import builders.OverseasRefundPensionSchemeBuilder.{
  anOverseasRefundPensionSchemeWithUkRefundCharge,
  anOverseasRefundPensionSchemeWithoutUkRefundCharge
}
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, emptyShortServiceRefundsViewModel}
import support.UnitTest

class ShortServiceRefundsViewModelSpec extends UnitTest { // scalatest:off magic.number

  ".isEmpty" should {
    "return true when no questions have been answered" in {
      emptyShortServiceRefundsViewModel.isEmpty
    }
    "return false when any questions have been answered" in {
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(true)).isEmpty shouldBe false
      aShortServiceRefundsViewModel.isEmpty shouldBe false
    }
  }

  ".isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aShortServiceRefundsViewModel.isFinished
      }
      "all required questions are answered" in {
        emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false)).isFinished shouldBe true
        emptyShortServiceRefundsViewModel
          .copy(
            shortServiceRefund = Some(true),
            shortServiceRefundCharge = Some(25),
            shortServiceRefundTaxPaid = Some(false),
            refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithUkRefundCharge, anOverseasRefundPensionSchemeWithoutUkRefundCharge)
          )
          .isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aShortServiceRefundsViewModel.copy(shortServiceRefundTaxPaidCharge = None).isFinished shouldBe false
        emptyShortServiceRefundsViewModel
          .copy(
            shortServiceRefund = Some(true),
            shortServiceRefundTaxPaid = Some(false)
          )
          .isFinished shouldBe false
        emptyShortServiceRefundsViewModel
          .copy(
            shortServiceRefund = Some(true),
            shortServiceRefundCharge = Some(25),
            shortServiceRefundTaxPaid = Some(false),
            refundPensionScheme = Seq(
              OverseasRefundPensionScheme(
                ukRefundCharge = Some(false),
                name = Some("Overseas Refund Scheme Name"),
                pensionSchemeTaxReference = None,
                qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
                providerAddress = Some("Scheme Address"),
                alphaTwoCountryCode = Some("FR"),
                alphaThreeCountryCode = Some("FRA")
              ),
              OverseasRefundPensionScheme(
                ukRefundCharge = Some(false),
                name = Some("Overseas Refund Scheme Name"),
                pensionSchemeTaxReference = Some("12345678RA"),
                qualifyingRecognisedOverseasPensionScheme = None,
                providerAddress = Some("Scheme Address"),
                alphaTwoCountryCode = None,
                alphaThreeCountryCode = None
              )
            )
          )
          .isFinished shouldBe false
      }
    }
  }

  ".journeyIsNo" should {
    "return true when shortServiceRefund is 'false'" in {
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(false)).journeyIsNo
    }
    "return false if shortServiceRefund is not 'false'" in {
      emptyShortServiceRefundsViewModel.journeyIsNo shouldBe false
      emptyShortServiceRefundsViewModel.copy(shortServiceRefund = Some(true)).journeyIsNo shouldBe false
    }
  }

  ".toOverseasPensionContributions" should {
    "transform a ShortServiceRefundsViewModel into a OverseasPensionContributions" in {
      val result = OverseasPensionContributions(
        overseasSchemeProvider = Seq(
          OverseasSchemeProvider(
            providerName = "Scheme Name with UK charge",
            providerAddress = "Scheme Address 1",
            providerCountryCode = "",
            qualifyingRecognisedOverseasPensionScheme = None,
            pensionSchemeTaxReference = Some(Seq("12345678RA"))
          ),
          OverseasSchemeProvider(
            providerName = "Scheme Name without UK charge",
            providerAddress = "Scheme Address 2",
            providerCountryCode = "FRA",
            qualifyingRecognisedOverseasPensionScheme = Some(Seq("Q123456")),
            pensionSchemeTaxReference = None
          )
        ),
        shortServiceRefund = 1999.99,
        shortServiceRefundTaxPaid = 1000.00
      )
      aShortServiceRefundsViewModel.toOverseasPensionContributions shouldBe result
    }
  }
}
