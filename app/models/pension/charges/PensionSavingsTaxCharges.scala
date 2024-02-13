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

// Minimum of one of these fields are required.
case class PensionSavingsTaxCharges(pensionSchemeTaxReference: Option[Seq[String]],
                                    lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[LifetimeAllowance],
                                    benefitInExcessOfLifetimeAllowance: Option[LifetimeAllowance])
    extends PensionChargesSubRequestModel {
  override def isEmpty: Boolean = this.productIterator.forall(_ == None)
}

object PensionSavingsTaxCharges {
  implicit val format: OFormat[PensionSavingsTaxCharges] = Json.format[PensionSavingsTaxCharges]

  def fromPriorData(prior: IncomeTaxUserData): PensionSavingsTaxCharges =
    PensionSavingsTaxCharges(
      pensionSchemeTaxReference = prior.pensions
        .flatMap(_.pensionCharges)
        .flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.pensionSchemeTaxReference),
      lumpSumBenefitTakenInExcessOfLifetimeAllowance = prior.pensions
        .flatMap(_.pensionCharges)
        .flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance),
      benefitInExcessOfLifetimeAllowance = prior.pensions
        .flatMap(_.pensionCharges)
        .flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.benefitInExcessOfLifetimeAllowance)
    )
}

case class EncryptedPensionSavingsTaxCharges(pensionSchemeTaxReference: Option[Seq[EncryptedValue]],
                                             lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance],
                                             benefitInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance])

object EncryptedPensionSavingsTaxCharges {
  implicit val format: OFormat[EncryptedPensionSavingsTaxCharges] = Json.format[EncryptedPensionSavingsTaxCharges]
}
