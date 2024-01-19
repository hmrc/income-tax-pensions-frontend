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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Charge(amount: BigDecimal, foreignTaxPaid: BigDecimal)

object Charge {
  implicit val format: OFormat[Charge] = Json.format[Charge]
}

case class EncryptedCharge(amount: EncryptedValue, foreignTaxPaid: EncryptedValue)

object EncryptedCharge {
  implicit val format: OFormat[EncryptedCharge] = Json.format[EncryptedCharge]
}
