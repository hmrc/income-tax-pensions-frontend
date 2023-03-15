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

package builders

import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}

object ShortServiceRefundsViewModelBuilder {
  //TODO: Update test data with correct values
  val aShortServiceRefundsViewModel = ShortServiceRefundsViewModel(
    shortServiceRefund = Some(true),
    shortServiceRefundCharge = Some(1999.99),
    shortServiceRefundTaxPaid = Some(true),
    shortServiceRefundTaxPaidCharge = Some(1000.00),
    refundPensionScheme = Seq(
      OverseasRefundPensionScheme(
        ukRefundCharge = Some(true),
        name = Some("Overseas Refund Scheme Name"),
        pensionSchemeTaxReference = None,
        qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
        providerAddress = Some("Scheme Address"),
        alphaTwoCountryCode = Some("FR"),
        alphaThreeCountryCode = Some("FRA")
      )
    )
  )

  val aShortServiceRefundsNonUkEmptySchemeViewModel = ShortServiceRefundsViewModel(
    shortServiceRefund = Some(true),
    shortServiceRefundCharge = Some(1999.99),
    shortServiceRefundTaxPaid = Some(true),
    shortServiceRefundTaxPaidCharge = Some(1000.00),
    refundPensionScheme = Seq(
      OverseasRefundPensionScheme(
        ukRefundCharge = Some(false)
      )
    )
  )
  val emptyShortServiceRefundsViewModel = ShortServiceRefundsViewModel()

  //TODO: shortServiceRefundTaxPaid shouldn't be part of minimal view model
  val minimalShortServiceRefundsViewModel = ShortServiceRefundsViewModel(shortServiceRefund = Option(false), shortServiceRefundTaxPaid = Option(false))
}
