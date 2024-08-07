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

package models.pension.employmentPensions

import play.api.libs.json.{Json, OFormat}

case class EmploymentPensions(employmentData: List[EmploymentPensionModel])

object EmploymentPensions {
  implicit val format: OFormat[EmploymentPensions] =
    Json.format[EmploymentPensions]

  val empty: EmploymentPensions =
    EmploymentPensions(List.empty[EmploymentPensionModel])
}

case class EncryptedEmploymentPensions(employmentData: List[EncryptedEmploymentPensionModel])

object EncryptedEmploymentPensions {
  implicit val format: OFormat[EncryptedEmploymentPensions] =
    Json.format[EncryptedEmploymentPensions]
}
