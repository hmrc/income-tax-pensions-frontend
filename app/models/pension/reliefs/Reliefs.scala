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

package models.pension.reliefs

import models.pension.PensionReliefsSubRequestModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Reliefs(regularPensionContributions: Option[BigDecimal],
                   oneOffPensionContributionsPaid: Option[BigDecimal],
                   retirementAnnuityPayments: Option[BigDecimal],
                   paymentToEmployersSchemeNoTaxRelief: Option[BigDecimal],
                   overseasPensionSchemeContributions: Option[BigDecimal])
    extends PensionReliefsSubRequestModel {
  override def isEmpty: Boolean = regularPensionContributions.isEmpty &&
    oneOffPensionContributionsPaid.isEmpty &&
    retirementAnnuityPayments.isEmpty &&
    paymentToEmployersSchemeNoTaxRelief.isEmpty &&
    overseasPensionSchemeContributions.isEmpty
}
object Reliefs {
  implicit val formats: OFormat[Reliefs] = Json.format[Reliefs]

  def empty: Reliefs = Reliefs(None, None, None, None, None)

}

case class EncryptedReliefs(regularPensionContributions: Option[EncryptedValue],
                            oneOffPensionContributionsPaid: Option[EncryptedValue],
                            retirementAnnuityPayments: Option[EncryptedValue],
                            paymentToEmployersSchemeNoTaxRelief: Option[EncryptedValue],
                            overseasPensionSchemeContributions: Option[EncryptedValue])

object EncryptedReliefs {
  implicit val formats: OFormat[EncryptedReliefs] = Json.format[EncryptedReliefs]
}
