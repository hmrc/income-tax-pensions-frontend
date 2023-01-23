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

import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}

object TransfersIntoOverseasPensionsViewModelBuilder {

  val aTransfersIntoOverseasPensionsViewModel = TransfersIntoOverseasPensionsViewModel(
    transfersIntoOverseas = Some(true),
    transferPensionSavings = Some(true),
    overseasTransferCharge = Some(true),
    overseasTransferChargeAmount = Some(1999.99),
    pensionSchemeTransferCharge = Some(true),
    pensionSchemeTransferChargeAmount = Some(1000.00),
    transferPensionScheme = Seq(
      TransferPensionScheme(
        ukTransferCharge = Some(false),
        name = Some("Foreign Scheme Name"),
        pensionSchemeTaxReference = None,
        qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
        providerAddress = Some("Scheme Address"),
        countryCode = Some("FRA")
      )
    )
  )
  val emptyTransfersIntoOverseasPensionsViewModel = TransfersIntoOverseasPensionsViewModel()

}
