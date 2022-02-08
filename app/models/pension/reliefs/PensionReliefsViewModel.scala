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

package models.pension.reliefs

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

//TODO fill the model with questions and answers for pension reliefs part of CYA
case class PensionReliefsViewModel(question1: Option[String], answer1: Option[BigDecimal])

object PensionReliefsViewModel {
  implicit val format: OFormat[PensionReliefsViewModel] = Json.format[PensionReliefsViewModel]
}

case class EncryptedPensionReliefsViewModel(question1: Option[EncryptedValue], answer1: Option[EncryptedValue])
object EncryptedPensionReliefsViewModel {
  implicit val format: OFormat[EncryptedPensionReliefsViewModel] = Json.format[EncryptedPensionReliefsViewModel]
}