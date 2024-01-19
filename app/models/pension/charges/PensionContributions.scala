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

package models.pension.charges

import models.pension.PensionChargesSubRequestModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionContributions(pensionSchemeTaxReference: Seq[String],
                                inExcessOfTheAnnualAllowance: BigDecimal,
                                annualAllowanceTaxPaid: BigDecimal) extends PensionChargesSubRequestModel {
  override def isEmpty: Boolean = false
}

object PensionContributions {
  implicit val format: OFormat[PensionContributions] = Json.format[PensionContributions]
}

case class EncryptedPensionContributions(pensionSchemeTaxReference: Seq[EncryptedValue],
                                         inExcessOfTheAnnualAllowance: EncryptedValue,
                                         annualAllowanceTaxPaid: EncryptedValue)

object EncryptedPensionContributions {
  implicit val format: OFormat[EncryptedPensionContributions] = Json.format[EncryptedPensionContributions]
}
