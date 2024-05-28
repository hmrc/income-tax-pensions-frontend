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

package testdata

import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}

object TransfersIntoOverseasPensionsViewModelTestData {

  val pstrReference = "12345678RA"
  val qopsReference = "123456"

  val transfersIntoOverseasPensionsAnswers = TransfersIntoOverseasPensionsViewModel(
    Some(true),
    Some(true),
    Some(1.0),
    Some(true),
    Some(2.0),
    Seq(transferPensionSchemeUK, transferPensionSchemeNonUK)
  )

  def transferPensionSchemeUK: TransferPensionScheme =
    TransferPensionScheme(Some(true), Some("UK Scheme"), Some(pstrReference), Some("Address"), Some("GB"), Some("GBR"))
  def transferPensionSchemeNonUK: TransferPensionScheme =
    TransferPensionScheme(Some(false), Some("Non-UK Scheme"), Some(qopsReference), Some("Address"), Some("FR"), Some("FRA"))

}
