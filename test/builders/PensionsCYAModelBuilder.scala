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

package builders

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.{aPaymentsIntoOverseasPensionsEmptyViewModel, aPaymentsIntoOverseasPensionsViewModel}
import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import builders.PensionAnnualAllowanceViewModelBuilder.{aPensionAnnualAllowanceEmptyViewModel, aPensionAnnualAllowanceViewModel}
import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import models.mongo.PensionsCYAModel
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PaymentsIntoOverseasPensionsViewModel, PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, TransfersIntoOverseasPensionsViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel

object PensionsCYAModelBuilder {

  val aPensionsCYAModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
    incomeFromPensions = anIncomeFromPensionsViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel,
    incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel,
    transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel
  )

  val aPensionsCYAEmptyModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsEmptyViewModel,
    incomeFromOverseasPensions = anIncomeFromOverseasPensionsEmptyViewModel,
    transfersIntoOverseasPensions = emptyTransfersIntoOverseasPensionsViewModel
  )

  val aPensionsCYAGeneratedFromPriorEmpty:PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel.copy(gateway = Some(true), totalPaymentsIntoRASQuestion = Some(true)),
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false)),
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false)),
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsEmptyViewModel,
    incomeFromOverseasPensions = anIncomeFromOverseasPensionsEmptyViewModel,
    transfersIntoOverseasPensions = emptyTransfersIntoOverseasPensionsViewModel
  )

  def paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionViewModel: PaymentsIntoPensionViewModel): PensionsCYAModel = {
    PensionsCYAModel(paymentsIntoPensionViewModel, PensionAnnualAllowancesViewModel(),
      PensionLifetimeAllowancesViewModel(), IncomeFromPensionsViewModel(),
      UnauthorisedPaymentsViewModel(), PaymentsIntoOverseasPensionsViewModel(),
      IncomeFromOverseasPensionsViewModel(), TransfersIntoOverseasPensionsViewModel())
  }

}
