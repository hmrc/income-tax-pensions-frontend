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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionCharges(submittedOn: String,
                          pensionSchemeOverseasTransfers: Option[PensionSchemeOverseasTransfers],
                          pensionSchemeUnauthorisedPayments: Option[PensionSchemeUnauthorisedPayments],
                          pensionContributions: Option[PensionContributions],
                          overseasPensionContributions: Option[OverseasPensionContributions]) {

  def isEmpty: Boolean  = this.productIterator.forall(_ == None)
  def nonEmpty: Boolean = !isEmpty
}

object PensionCharges {
  implicit val format: OFormat[PensionCharges] = Json.format[PensionCharges]
}

case class EncryptedPensionCharges(submittedOn: EncryptedValue,
                                   pensionSchemeOverseasTransfers: Option[EncryptedPensionSchemeOverseasTransfers],
                                   pensionSchemeUnauthorisedPayments: Option[EncryptedPensionSchemeUnauthorisedPayments],
                                   pensionContributions: Option[EncryptedPensionContributions],
                                   overseasPensionContributions: Option[EncryptedOverseasPensionContributions]) {}

object EncryptedPensionCharges {
  implicit val format: OFormat[EncryptedPensionCharges] = Json.format[EncryptedPensionCharges]
}
