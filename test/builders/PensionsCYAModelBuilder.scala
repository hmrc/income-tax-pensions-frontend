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
import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import builders.PensionAnnualAllowanceViewModelBuilder.{aPensionAnnualAllowanceEmptyViewModel, aPensionAnnualAllowanceViewModel}
import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import forms.No
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import models.mongo.PensionsCYAModel
import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel

object PensionsCYAModelBuilder {

  val aPensionsCYAModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
    incomeFromPensions = anIncomeFromPensionsViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsViewModel
  )

  val aPensionsCYAEmptyModel: PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel,
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel,
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel,
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel

  )

  val aPensionsCYAGeneratedFromPriorEmpty:PensionsCYAModel = PensionsCYAModel(
    paymentsIntoPension = aPaymentsIntoPensionsEmptyViewModel.copy(totalPaymentsIntoRASQuestion = Some(true)),
    pensionsAnnualAllowances = aPensionAnnualAllowanceEmptyViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(No.toString)),
    pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel,
    incomeFromPensions = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false)),
    unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel
  )

  def paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionViewModel: PaymentsIntoPensionViewModel): PensionsCYAModel = {
    PensionsCYAModel(paymentsIntoPensionViewModel, PensionAnnualAllowancesViewModel(),
      PensionLifetimeAllowancesViewModel(), IncomeFromPensionsViewModel(), UnauthorisedPaymentsViewModel())
  }

}
