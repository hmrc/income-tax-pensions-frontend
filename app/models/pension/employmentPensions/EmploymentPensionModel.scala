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

package models.pension.employmentPensions

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class EmploymentPensionModel(employmentId: String,
                                  pensionSchemeName: String,
                                  pensionSchemeRef: Option[String],
                                  pensionId: Option[String],
                                  startDate: Option[String],
                                  endDate: Option[String],
                                  amount: Option[BigDecimal],
                                  taxPaid: Option[BigDecimal],
                                  isCustomerEmploymentData: Option[Boolean]
                                 )

object EmploymentPensionModel {
  implicit val format: OFormat[EmploymentPensionModel] = Json.format[EmploymentPensionModel]
}

case class EncryptedEmploymentPensionModel(employmentId: EncryptedValue,
                                           pensionSchemeName: EncryptedValue,
                                           pensionSchemeRef: Option[EncryptedValue],
                                           pensionId: Option[EncryptedValue],
                                           startDate: Option[EncryptedValue],
                                           endDate: Option[EncryptedValue],
                                           amount: Option[EncryptedValue],
                                           taxPaid: Option[EncryptedValue],
                                           isCustomerEmploymentData: Option[EncryptedValue]
                                          )

object EncryptedEmploymentPensionModel {
  implicit val format: OFormat[EncryptedEmploymentPensionModel] = Json.format[EncryptedEmploymentPensionModel]
}
