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

package models.mongo

import models.pension.charges._
import models.pension.reliefs.{EncryptedPaymentsIntoPensionViewModel, PaymentsIntoPensionsViewModel}
import models.pension.statebenefits.{EncryptedIncomeFromPensionsViewModel, IncomeFromPensionsViewModel}
import play.api.libs.json.{Json, OFormat}

case class PensionsCYAModel(paymentsIntoPension: PaymentsIntoPensionsViewModel,
                            pensionsAnnualAllowances: PensionAnnualAllowancesViewModel,
                            pensionLifetimeAllowances: PensionLifetimeAllowancesViewModel,
                            incomeFromPensions: IncomeFromPensionsViewModel,
                            unauthorisedPayments: UnauthorisedPaymentsViewModel,
                            paymentsIntoOverseasPensions: PaymentsIntoOverseasPensionsViewModel,
                            incomeFromOverseasPensions: IncomeFromOverseasPensionsViewModel,
                            transfersIntoOverseasPensions: TransfersIntoOverseasPensionsViewModel,
                            shortServiceRefunds: ShortServiceRefundsViewModel) {
  
  def isEmpty: Boolean = paymentsIntoPension.isEmpty && pensionsAnnualAllowances.isEmpty && pensionLifetimeAllowances.isEmpty &&
    incomeFromPensions.isEmpty && paymentsIntoOverseasPensions.isEmpty &&
      incomeFromOverseasPensions.isEmpty && transfersIntoOverseasPensions.isEmpty && shortServiceRefunds.isEmpty
}

object PensionsCYAModel {
  implicit val format: OFormat[PensionsCYAModel] = Json.format[PensionsCYAModel]

  def emptyModels: PensionsCYAModel = PensionsCYAModel(
    PaymentsIntoPensionsViewModel(),
    PensionAnnualAllowancesViewModel(),
    PensionLifetimeAllowancesViewModel(),
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
                                    pensionLifetimeAllowances: EncryptedPensionLifetimeAllowancesViewModel,
                                    incomeFromPensions: EncryptedIncomeFromPensionsViewModel,
                                    unauthorisedPayments: EncryptedUnauthorisedPaymentsViewModel,
                                    paymentsIntoOverseasPensions: EncryptedPaymentsIntoOverseasPensionsViewModel,
                                    incomeFromOverseasPensions: EncryptedIncomeFromOverseasPensionsViewModel,
                                    transfersIntoOverseasPensions: EncryptedTransfersIntoOverseasPensionsViewModel,
                                    shortServiceRefunds: EncryptedShortServiceRefundsViewModel
                                   )

object EncryptedPensionCYAModel {
  implicit val format: OFormat[EncryptedPensionCYAModel] = Json.format[EncryptedPensionCYAModel]
}
