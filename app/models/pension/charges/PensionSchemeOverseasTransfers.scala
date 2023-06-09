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

import models.pension.PensionChargesRequestSubModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionSchemeOverseasTransfers(overseasSchemeProvider: Seq[OverseasSchemeProvider],
                                          transferCharge: BigDecimal,
                                          transferChargeTaxPaid: BigDecimal) extends PensionChargesRequestSubModel {
  override def isEmpty: Boolean = false
}

object PensionSchemeOverseasTransfers {
  implicit val format: OFormat[PensionSchemeOverseasTransfers] = Json.format[PensionSchemeOverseasTransfers]
}

case class EncryptedPensionSchemeOverseasTransfers(overseasSchemeProvider: Seq[EncryptedOverseasSchemeProvider],
                                                   transferCharge: EncryptedValue,
                                                   transferChargeTaxPaid: EncryptedValue)

object EncryptedPensionSchemeOverseasTransfers {
  implicit val format: OFormat[EncryptedPensionSchemeOverseasTransfers] = Json.format[EncryptedPensionSchemeOverseasTransfers]
}

