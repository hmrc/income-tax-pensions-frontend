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

package models.pension.charges

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionSavingsTaxCharges(pensionSchemeTaxReference: Seq[String],
                                    lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[LifetimeAllowance],
                                    benefitInExcessOfLifetimeAllowance: Option[LifetimeAllowance],
                                    isAnnualAllowanceReduced: Boolean,
                                    taperedAnnualAllowance: Option[Boolean],
                                    moneyPurchasedAllowance: Option[Boolean])

object PensionSavingsTaxCharges {
  implicit val format: OFormat[PensionSavingsTaxCharges] = Json.format[PensionSavingsTaxCharges]
}

case class EncryptedPensionSavingsTaxCharges(pensionSchemeTaxReference: Seq[EncryptedValue],
                                             lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance],
                                             benefitInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance],
                                             isAnnualAllowanceReduced: EncryptedValue,
                                             taperedAnnualAllowance: Option[EncryptedValue],
                                             moneyPurchasedAllowance: Option[EncryptedValue])

object EncryptedPensionSavingsTaxCharges {
  implicit val format: OFormat[EncryptedPensionSavingsTaxCharges] = Json.format[EncryptedPensionSavingsTaxCharges]
}
