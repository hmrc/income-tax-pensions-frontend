/*
 * Copyright 2022 HM Revenue & Customs
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

package models.pension.statebenefits

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

//TODO fill the model with questions and answers for state benefits part of CYA
case class StateBenefitsViewModel(question1: Option[String], answer1: Option[BigDecimal])

object StateBenefitsViewModel {
  implicit val format: OFormat[StateBenefitsViewModel] = Json.format[StateBenefitsViewModel]
}

case class EncryptedStateBenefitsViewModel(question1: Option[EncryptedValue], answer1: Option[EncryptedValue])

object EncryptedStateBenefitsViewModel {
  implicit val format: OFormat[EncryptedStateBenefitsViewModel] = Json.format[EncryptedStateBenefitsViewModel]
}
