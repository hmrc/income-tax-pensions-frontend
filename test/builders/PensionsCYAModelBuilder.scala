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

package builders

import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.{aPaymentsIntoOverseasPensionsViewModel, aPaymentsIntoOverseasPensionsEmptyViewModel}
import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import builders.PensionAnnualAllowanceViewModelBuilder.{aPensionAnnualAllowanceEmptyViewModel, aPensionAnnualAllowanceViewModel}
import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import models.mongo.PensionsCYAModel
import models.pension.charges.{PaymentsIntoOverseasPensionsViewModel, PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel

object PensionsCYAModelBuilder {

  val aPensionsCYAModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
    incomeFromPensions = anIncomeFromPensionsViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
  )

  val aPensionsCYAEmptyModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsEmptyViewModel
  )

  val aPensionsCYAGeneratedFromPriorEmpty:PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel.copy(gateway = Some(true), totalPaymentsIntoRASQuestion = Some(true)),
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false)),
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false)),
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel,
    paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
  )

  def paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionViewModel: PaymentsIntoPensionViewModel): PensionsCYAModel = {
    PensionsCYAModel(paymentsIntoPensionViewModel, PensionAnnualAllowancesViewModel(),
      PensionLifetimeAllowancesViewModel(), IncomeFromPensionsViewModel(), UnauthorisedPaymentsViewModel(), PaymentsIntoOverseasPensionsViewModel())
  }

}
