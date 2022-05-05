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

package models.pension

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class EmploymentPensions(hmrcEmploymentData: Seq[EmploymentPensionModel],
                              customerEmploymentData: Seq[EmploymentPensionModel]
                             )

object EmploymentPensions {
  implicit val format: OFormat[EmploymentPensions] = Json.format[EmploymentPensions]
}

case class EmploymentPensionModel(employmentId: String,
                                  pensionSchemeName: String,
                                  pensionSchemeRef: Option[String],
                                  pensionId: Option[String],
                                  startDate: Option[String],
                                  endDate: Option[String],
                                  amount: Option[BigDecimal],
                                  taxAmount: Option[BigDecimal],
                                  occPen: Option[Boolean]
                                 )

object EmploymentPensionModel {
  implicit val reads: Reads[EmploymentPensionModel] = (
    (__ \\ "employmentId").read[String] and
      (__ \\ "employerName").read[String] and
      (__ \\ "employerRef").readNullable[String] and
      (__ \\ "payrollId").readNullable[String] and
      (__ \\ "startDate").readNullable[String] and
      (__ \\ "cessationDate").readNullable[String] and
      (__ \\ "taxablePayToDate").readNullable[BigDecimal] and
      (__ \\ "totalTaxToDate").readNullable[BigDecimal] and
      (__ \\ "occPen").readNullable[Boolean]
    )(EmploymentPensionModel.apply _)

  implicit val writes: Writes[EmploymentPensionModel] = Json.writes[EmploymentPensionModel]
}
