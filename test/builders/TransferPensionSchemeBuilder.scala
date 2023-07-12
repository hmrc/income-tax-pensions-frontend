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

import models.pension.charges.TransferPensionScheme

object TransferPensionSchemeBuilder {

  val aUkTransferPensionScheme = TransferPensionScheme(
    ukTransferCharge = Some(true),
    name = Some("UK TPS"),
    pstr = Some("12345678RA"),
    qops = None,
    providerAddress = Some("Some address 1"),
    alphaTwoCountryCode = None,
    alphaThreeCountryCode = None
  )

  val aNonUkTransferPensionScheme = TransferPensionScheme(
    ukTransferCharge = Some(false),
    name = Some("Non-UK TPS"),
    pstr = None,
    qops = Some("QOPS123456"),
    providerAddress = Some("Some address 2"),
    alphaTwoCountryCode = Some("FR"),
    alphaThreeCountryCode = Some("FRA")
  )

}
