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

import builders.StateBenefitViewModelBuilder.{
  aStatePensionLumpSumNoAddToCalculationViewModel,
  aStatePensionLumpSumViewModel,
  aStatePensionNoAddToCalculationViewModel,
  aStatePensionViewModel,
  anStateBenefitViewModel,
  anStateBenefitViewModelOne
}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import models.pension.statebenefits.IncomeFromPensionsViewModel

object IncomeFromPensionsViewModelBuilder {

  val anIncomeFromPensionsViewModel: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = Some(aStatePensionViewModel),
    statePensionLumpSum = Some(aStatePensionLumpSumViewModel),
    uKPensionIncomesQuestion = Some(true),
    uKPensionIncomes = Some(List(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
  )
  val aStatePensionIncomeFromPensionsViewModel: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = Some(aStatePensionViewModel),
    statePensionLumpSum = Some(aStatePensionLumpSumViewModel),
    uKPensionIncomesQuestion = None,
    uKPensionIncomes = Some(List.empty)
  )

  val aStatePensionIncomeFromPensionsNoAddToCalculationViewModel: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = Some(aStatePensionNoAddToCalculationViewModel),
    statePensionLumpSum = Some(aStatePensionLumpSumNoAddToCalculationViewModel),
    uKPensionIncomesQuestion = None,
    uKPensionIncomes = Some(List.empty)
  )

  val statePensionOnly: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = Some(anStateBenefitViewModel),
    statePensionLumpSum = None,
    uKPensionIncomesQuestion = None,
    uKPensionIncomes = Some(List.empty)
  )

  val statePensionLumpSumOnly: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = None,
    statePensionLumpSum = Some(anStateBenefitViewModel),
    uKPensionIncomesQuestion = None,
    uKPensionIncomes = Some(List.empty)
  )

  val spAndSpLumpSum: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = Some(anStateBenefitViewModelOne),
    statePensionLumpSum = Some(anStateBenefitViewModel),
    uKPensionIncomesQuestion = None,
    uKPensionIncomes = Some(List.empty)
  )

  val aUKIncomeFromPensionsViewModel: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = None,
    statePensionLumpSum = None,
    uKPensionIncomesQuestion = Some(true),
    uKPensionIncomes = Some(List(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
  )

  val viewModelSingularClaim: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel(
    statePension = None,
    statePensionLumpSum = None,
    uKPensionIncomesQuestion = Some(true),
    uKPensionIncomes = Some(List(anUkPensionIncomeViewModelOne))
  )

  val anIncomeFromPensionEmptyViewModel: IncomeFromPensionsViewModel = IncomeFromPensionsViewModel()

}
