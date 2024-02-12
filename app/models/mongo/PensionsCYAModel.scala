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

package models.mongo

import models.pension.charges._
import models.pension.reliefs.{EncryptedPaymentsIntoPensionViewModel, PaymentsIntoPensionsViewModel}
import models.pension.statebenefits.{EncryptedIncomeFromPensionsViewModel, IncomeFromPensionsViewModel}
import play.api.libs.json.{Json, OFormat}

case class PensionsCYAModel(paymentsIntoPension: PaymentsIntoPensionsViewModel,
                            pensionsAnnualAllowances: PensionAnnualAllowancesViewModel,
                            incomeFromPensions: IncomeFromPensionsViewModel,
                            unauthorisedPayments: UnauthorisedPaymentsViewModel,
                            paymentsIntoOverseasPensions: PaymentsIntoOverseasPensionsViewModel,
                            incomeFromOverseasPensions: IncomeFromOverseasPensionsViewModel,
                            transfersIntoOverseasPensions: TransfersIntoOverseasPensionsViewModel,
                            shortServiceRefunds: ShortServiceRefundsViewModel) {

  def isEmpty: Boolean = paymentsIntoPension.isEmpty && pensionsAnnualAllowances.isEmpty &&
    incomeFromPensions.isEmpty && paymentsIntoOverseasPensions.isEmpty &&
    incomeFromOverseasPensions.isEmpty && transfersIntoOverseasPensions.isEmpty && shortServiceRefunds.isEmpty

  def nonEmpty: Boolean = !isEmpty

  /* It merges the current model with the overrides. It favors the overrides over the current model fields if they exists.
   * It means that the user has changed the data but not yet submitted */
  def merge(overrides: Option[PensionsCYAModel]): PensionsCYAModel = {
    val overridesPaymentsIntoPension           = overrides.map(_.paymentsIntoPension).getOrElse(PaymentsIntoPensionsViewModel())
    val overridesPensionsAnnualAllowances      = overrides.map(_.pensionsAnnualAllowances).getOrElse(PensionAnnualAllowancesViewModel())
    val overridesIncomeFromPensions            = overrides.map(_.incomeFromPensions).getOrElse(IncomeFromPensionsViewModel())
    val overridesUnauthorisedPayments          = overrides.map(_.unauthorisedPayments).getOrElse(UnauthorisedPaymentsViewModel())
    val overridesPaymentsIntoOverseasPensions  = overrides.map(_.paymentsIntoOverseasPensions).getOrElse(PaymentsIntoOverseasPensionsViewModel())
    val overridesIncomeFromOverseasPensions    = overrides.map(_.incomeFromOverseasPensions).getOrElse(IncomeFromOverseasPensionsViewModel())
    val overridesTransfersIntoOverseasPensions = overrides.map(_.transfersIntoOverseasPensions).getOrElse(TransfersIntoOverseasPensionsViewModel())
    val overridesShortServiceRefunds           = overrides.map(_.shortServiceRefunds).getOrElse(ShortServiceRefundsViewModel())

    copy(
      paymentsIntoPension = if (overridesPaymentsIntoPension.nonEmpty) overridesPaymentsIntoPension else paymentsIntoPension,
      pensionsAnnualAllowances = if (overridesPensionsAnnualAllowances.nonEmpty) overridesPensionsAnnualAllowances else pensionsAnnualAllowances,
      incomeFromPensions = if (overridesIncomeFromPensions.nonEmpty) overridesIncomeFromPensions else incomeFromPensions,
      unauthorisedPayments = if (overridesUnauthorisedPayments.nonEmpty) overridesUnauthorisedPayments else unauthorisedPayments,
      paymentsIntoOverseasPensions =
        if (overridesPaymentsIntoOverseasPensions.nonEmpty) overridesPaymentsIntoOverseasPensions else paymentsIntoOverseasPensions,
      incomeFromOverseasPensions =
        if (overridesIncomeFromOverseasPensions.nonEmpty) overridesIncomeFromOverseasPensions else incomeFromOverseasPensions,
      transfersIntoOverseasPensions =
        if (overridesTransfersIntoOverseasPensions.nonEmpty) overridesTransfersIntoOverseasPensions else transfersIntoOverseasPensions,
      shortServiceRefunds = if (overridesShortServiceRefunds.nonEmpty) overridesShortServiceRefunds else shortServiceRefunds
    )
  }

  def removePaymentsIntoPension: PensionsCYAModel =
    copy(paymentsIntoPension = PaymentsIntoPensionsViewModel())
}

object PensionsCYAModel {
  implicit val format: OFormat[PensionsCYAModel] = Json.format[PensionsCYAModel]

  def emptyModels: PensionsCYAModel = PensionsCYAModel(
    PaymentsIntoPensionsViewModel(),
    PensionAnnualAllowancesViewModel(),
    IncomeFromPensionsViewModel(),
    UnauthorisedPaymentsViewModel(),
    PaymentsIntoOverseasPensionsViewModel(),
    IncomeFromOverseasPensionsViewModel(),
    TransfersIntoOverseasPensionsViewModel(),
    ShortServiceRefundsViewModel()
  )
}

case class EncryptedPensionCYAModel(encryptedPaymentsIntoPension: EncryptedPaymentsIntoPensionViewModel,
                                    encryptedPensionAnnualAllowances: EncryptedPensionAnnualAllowancesViewModel,
                                    incomeFromPensions: EncryptedIncomeFromPensionsViewModel,
                                    unauthorisedPayments: EncryptedUnauthorisedPaymentsViewModel,
                                    paymentsIntoOverseasPensions: EncryptedPaymentsIntoOverseasPensionsViewModel,
                                    incomeFromOverseasPensions: EncryptedIncomeFromOverseasPensionsViewModel,
                                    transfersIntoOverseasPensions: EncryptedTransfersIntoOverseasPensionsViewModel,
                                    shortServiceRefunds: EncryptedShortServiceRefundsViewModel)

object EncryptedPensionCYAModel {
  implicit val format: OFormat[EncryptedPensionCYAModel] = Json.format[EncryptedPensionCYAModel]
}
