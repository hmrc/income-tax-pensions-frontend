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

import builders.TransferPensionSchemeBuilder.{aNonUkTransferPensionScheme, aUkTransferPensionScheme}
import models.pension.charges.TransfersIntoOverseasPensionsViewModel

object TransfersIntoOverseasPensionsViewModelBuilder {

  val aTransfersIntoOverseasPensionsViewModel = TransfersIntoOverseasPensionsViewModel(
    transferPensionSavings = Some(true),
    overseasTransferCharge = Some(true),
    overseasTransferChargeAmount = Some(1999.99),
    pensionSchemeTransferCharge = Some(true),
    pensionSchemeTransferChargeAmount = Some(1000.00),
    transferPensionScheme = Seq(aUkTransferPensionScheme, aNonUkTransferPensionScheme)
  )

  val aTransfersIntoOverseasPensionsAnotherViewModel = TransfersIntoOverseasPensionsViewModel(
    transferPensionSavings = Some(false),
    overseasTransferCharge = Some(false),
    overseasTransferChargeAmount = Some(1),
    pensionSchemeTransferCharge = Some(false),
    pensionSchemeTransferChargeAmount = Some(2),
    transferPensionScheme = Seq(aUkTransferPensionScheme)
  )

  val viewModelNoSchemeDetails = TransfersIntoOverseasPensionsViewModel(
    transferPensionSavings = Some(false),
    overseasTransferCharge = None,
    overseasTransferChargeAmount = None,
    pensionSchemeTransferCharge = None,
    pensionSchemeTransferChargeAmount = None,
    transferPensionScheme = Seq.empty
  )

  val emptyTransfersIntoOverseasPensionsViewModel = TransfersIntoOverseasPensionsViewModel()

}
