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
  aPriorStatePensionLumpSumViewModel,
  aPriorStatePensionViewModel,
  aStatePensionLumpSumViewModel,
  aStatePensionViewModel
}
import models.pension.statebenefits.ClaimCYAModel

import java.time.LocalDate

object ClaimCYAModelBuilder {

  val aStatePensionClaimCYAModel = ClaimCYAModel(
    benefitId = aStatePensionViewModel.benefitId,
    startDate = aStatePensionViewModel.startDate.getOrElse(LocalDate.now()),
    amount = aStatePensionViewModel.amount,
    taxPaidQuestion = aStatePensionViewModel.taxPaidQuestion,
    taxPaid = aStatePensionViewModel.taxPaid
  )

  val aStatePensionLumpSumClaimCYAModel = ClaimCYAModel(
    benefitId = aStatePensionLumpSumViewModel.benefitId,
    startDate = aStatePensionLumpSumViewModel.startDate.getOrElse(LocalDate.now()),
    amount = aStatePensionLumpSumViewModel.amount,
    taxPaidQuestion = aStatePensionLumpSumViewModel.taxPaidQuestion,
    taxPaid = aStatePensionLumpSumViewModel.taxPaid
  )

  val aPriorStatePensionClaimCYAModel = ClaimCYAModel(
    benefitId = aPriorStatePensionViewModel.benefitId,
    startDate = aPriorStatePensionViewModel.startDate.getOrElse(LocalDate.now()),
    amount = aPriorStatePensionViewModel.amount,
    taxPaidQuestion = aPriorStatePensionViewModel.taxPaidQuestion,
    taxPaid = aPriorStatePensionViewModel.taxPaid
  )

  val aPriorStatePensionLumpSumClaimCYAModel = ClaimCYAModel(
    benefitId = aPriorStatePensionLumpSumViewModel.benefitId,
    startDate = aPriorStatePensionLumpSumViewModel.startDate.getOrElse(LocalDate.now()),
    amount = aPriorStatePensionLumpSumViewModel.amount,
    taxPaidQuestion = aPriorStatePensionLumpSumViewModel.taxPaidQuestion,
    taxPaid = aPriorStatePensionLumpSumViewModel.taxPaid
  )

}
