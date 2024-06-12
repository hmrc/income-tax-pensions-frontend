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

import connectors.OptionalContentHttpReads
import models.mongo.PensionsCYAModel
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionAnnualAllowancesViewModel(reducedAnnualAllowanceQuestion: Option[Boolean] = None,
                                            moneyPurchaseAnnualAllowance: Option[Boolean] = None,
                                            taperedAnnualAllowance: Option[Boolean] = None,
                                            aboveAnnualAllowanceQuestion: Option[Boolean] = None,
                                            aboveAnnualAllowance: Option[BigDecimal] = None,
                                            pensionProvidePaidAnnualAllowanceQuestion: Option[Boolean] = None,
                                            taxPaidByPensionProvider: Option[BigDecimal] = None,
                                            pensionSchemeTaxReferences: Option[Seq[String]] = None)
    extends PensionCYABaseModel {

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean =
    reducedAnnualAllowanceQuestion.exists(x =>
      !x || {
        (moneyPurchaseAnnualAllowance.getOrElse(false) || taperedAnnualAllowance.getOrElse(false)) &&
        aboveAnnualAllowanceQuestion.exists(x =>
          !x || {
            aboveAnnualAllowance.isDefined &&
            pensionProvidePaidAnnualAllowanceQuestion.exists(x =>
              !x || {
                taxPaidByPensionProvider.isDefined && pensionSchemeTaxReferences.exists(_.nonEmpty)
              })
          })
      })

  def journeyIsNo: Boolean =
    (!reducedAnnualAllowanceQuestion.getOrElse(true)
      && moneyPurchaseAnnualAllowance.isEmpty
      && taperedAnnualAllowance.isEmpty
      && aboveAnnualAllowanceQuestion.isEmpty
      && aboveAnnualAllowance.isEmpty
      && pensionProvidePaidAnnualAllowanceQuestion.isEmpty
      && taxPaidByPensionProvider.isEmpty
      && pensionSchemeTaxReferences.isEmpty)

  def journeyIsUnanswered: Boolean = this.isEmpty

  def typeOfAllowance: Option[Seq[String]] =
    (moneyPurchaseAnnualAllowance, taperedAnnualAllowance) match {
      case (Some(true), Some(true)) => Some(Seq("Money purchase", "Tapered"))
      case (Some(true), _)          => Some(Seq("Money purchase"))
      case (_, Some(true))          => Some(Seq("Tapered"))
      case _                        => None
    }

  def toPensionsCYAModel: PensionsCYAModel = PensionsCYAModel.emptyModels.copy(pensionsAnnualAllowances = this)
}

object PensionAnnualAllowancesViewModel {
  implicit val format: OFormat[PensionAnnualAllowancesViewModel]                  = Json.format[PensionAnnualAllowancesViewModel]
  implicit val optRds: OptionalContentHttpReads[PensionAnnualAllowancesViewModel] = new OptionalContentHttpReads[PensionAnnualAllowancesViewModel]

  def empty: PensionAnnualAllowancesViewModel = PensionAnnualAllowancesViewModel(None, None, None, None, None, None, None, None)
}

case class EncryptedPensionAnnualAllowancesViewModel(reducedAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     moneyPurchaseAnnualAllowance: Option[EncryptedValue] = None,
                                                     taperedAnnualAllowance: Option[EncryptedValue] = None,
                                                     aboveAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     aboveAnnualAllowance: Option[EncryptedValue] = None,
                                                     pensionProvidePaidAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     taxPaidByPensionProvider: Option[EncryptedValue] = None,
                                                     pensionSchemeTaxReferences: Option[Seq[EncryptedValue]] = None)

object EncryptedPensionAnnualAllowancesViewModel {
  implicit val format: OFormat[EncryptedPensionAnnualAllowancesViewModel] = Json.format[EncryptedPensionAnnualAllowancesViewModel]
}
