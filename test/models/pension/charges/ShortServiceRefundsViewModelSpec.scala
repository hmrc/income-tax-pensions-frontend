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

import builders.OverseasRefundPensionSchemeBuilder.anOverseasRefundPensionScheme
import builders.ShortServiceRefundsViewModelBuilder._
import cats.implicits.{catsSyntaxOptionId, none}
import support.UnitTest

class ShortServiceRefundsViewModelSpec extends UnitTest { // scalatest:off magic.number

  ".maybeToDownstreamModel" when {
    "claiming a refund charge and providing schemes details" should {
      "generate an OverseasSchemeProvider model for each of the schemes" in {
        val expectedResult = OverseasPensionContributions(
          overseasSchemeProvider = Seq(
            OverseasSchemeProvider(
              providerName = "Scheme Name without UK charge",
              providerAddress = "Scheme Address 2",
              providerCountryCode = "FRA",
              qualifyingRecognisedOverseasPensionScheme = Some(Seq("Q123456")),
              pensionSchemeTaxReference = None
            )
          ),
          shortServiceRefund = refundCharge,
          shortServiceRefundTaxPaid = refundTaxPaidCharge
        )

        aShortServiceRefundsViewModel.maybeToDownstreamModel shouldBe expectedResult.some
      }
      "return None if there are any missing mandatory fields" in {
        viewModelMissingMandatorySchemeFields.maybeToDownstreamModel shouldBe none[OverseasPensionContributions]
      }
    }
    "neither a refund charge nor scheme details are provided" should {
      "return None" in {
        minimalShortServiceRefundsViewModel.maybeToDownstreamModel shouldBe none[OverseasPensionContributions]
      }
    }
  }

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
            refundPensionScheme = Seq(anOverseasRefundPensionScheme)
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
                name = Some("Overseas Refund Scheme Name"),
                qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
                providerAddress = Some("Scheme Address"),
                alphaTwoCountryCode = Some("FR"),
                alphaThreeCountryCode = Some("FRA")
              ),
              OverseasRefundPensionScheme(
                name = Some("Overseas Refund Scheme Name"),
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
}
