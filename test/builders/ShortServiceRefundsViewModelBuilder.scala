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

import builders.OverseasRefundPensionSchemeBuilder.anOverseasRefundPensionScheme
import models.pension.charges.ShortServiceRefundsViewModel

object ShortServiceRefundsViewModelBuilder {

  val refundCharge        = 1999.99
  val refundTaxPaidCharge = 1000.00

  val aShortServiceRefundsViewModel: ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel(
      shortServiceRefund = Some(true),
      shortServiceRefundCharge = Some(refundCharge),
      shortServiceRefundTaxPaid = Some(true),
      shortServiceRefundTaxPaidCharge = Some(refundTaxPaidCharge),
      refundPensionScheme = Seq(anOverseasRefundPensionScheme)
    )

  val viewModelMissingMandatorySchemeFields: ShortServiceRefundsViewModel = {
    val schemeWithMissingFields = anOverseasRefundPensionScheme.copy(name = None)

    aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(schemeWithMissingFields))
  }

  val aShortServiceRefundsEmptySchemeViewModel: ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel(
      shortServiceRefund = Some(true),
      shortServiceRefundCharge = Some(refundCharge),
      shortServiceRefundTaxPaid = Some(true),
      shortServiceRefundTaxPaidCharge = Some(refundTaxPaidCharge),
      refundPensionScheme = Seq.empty
    )

  val emptyShortServiceRefundsViewModel: ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel()

  val minimalShortServiceRefundsViewModel: ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel(shortServiceRefund = Some(false))
}
