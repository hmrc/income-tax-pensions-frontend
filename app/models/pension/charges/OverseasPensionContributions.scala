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

import models.IncomeTaxUserData
import models.pension.PensionChargesSubRequestModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class OverseasPensionContributions(overseasSchemeProvider: Seq[OverseasSchemeProvider],
                                        shortServiceRefund: BigDecimal,
                                        shortServiceRefundTaxPaid: BigDecimal)
    extends PensionChargesSubRequestModel {
  override def isEmpty: Boolean = false
}

object OverseasPensionContributions {
  implicit val format: OFormat[OverseasPensionContributions] = Json.format[OverseasPensionContributions]

  def fromPriorData(prior: IncomeTaxUserData): Option[OverseasPensionContributions] =
    prior.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
}

case class EncryptedOverseasPensionContributions(overseasSchemeProvider: Seq[EncryptedOverseasSchemeProvider],
                                                 shortServiceRefund: EncryptedValue,
                                                 shortServiceRefundTaxPaid: EncryptedValue)

object EncryptedOverseasPensionContributions {
  implicit val format: OFormat[EncryptedOverseasPensionContributions] = Json.format[EncryptedOverseasPensionContributions]
}
