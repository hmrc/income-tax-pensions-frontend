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

import models.pension.statebenefits.StateBenefitViewModel

import java.time.{Instant, LocalDate}
import java.util.UUID

object StateBenefitViewModelBuilder {

  val anStateBenefitViewModelOne: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = Some(UUID.fromString("a9e8057e-fbbc-47a8-a8b4-78d9f015c934")),
    startDateQuestion = Some(true),
    startDate = Some(LocalDate.parse("2019-11-13")),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = Some(true),
    submittedOn = Some(Instant.parse("2020-09-11T17:23:00Z")),
    endDateQuestion = Some(true),
    endDate = Some(LocalDate.parse("2045-08-28")),
    amountPaidQuestion = Some(true),
    amount = Some(155.88),
    taxPaidQuestion = Some(true),
    taxPaid = Some(8.99)
  )

  val anStateBenefitViewModelTwo: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = Some(UUID.fromString("a9e8057e-fbbc-47a8-a8b4-78d9f015c935")),
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = Some(true),
    dateIgnored = Some(Instant.parse("2019-12-18T17:23:00Z")),
    submittedOnQuestion = Some(true),
    submittedOn = Some(Instant.parse("2020-09-10T17:23:00Z")),
    endDateQuestion = Some(true),
    endDate = Some(LocalDate.parse("2045-09-28")),
    amountPaidQuestion = Some(true),
    amount = Some(166.88),
    taxPaidQuestion = Some(true),
    taxPaid = Some(3.99)
  )

  val aStatePensionViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(166.88),
    taxPaidQuestion = None,
    taxPaid = None
  )

  val aStatePensionLumpSumViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(200.00),
    taxPaidQuestion = Some(true),
    taxPaid = Some(50.00)
  )

  val aPriorStatePensionViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = Some(UUID.fromString("558238ef-d2ff-4839-bd6d-307324d6fe37")),
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(166.88),
    taxPaidQuestion = None,
    taxPaid = None
  )

  val aPriorStatePensionLumpSumViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = Some(UUID.fromString("558238ef-d2ff-4839-bd6d-307324d6fe37")),
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(200.00),
    taxPaidQuestion = Some(true),
    taxPaid = Some(50.00)
  )

  val aStatePensionNoAddToCalculationViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(166.88),
    taxPaidQuestion = None,
    taxPaid = None
  )

  val aStatePensionLumpSumNoAddToCalculationViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = Some(LocalDate.parse("2019-11-14")),
    startDateQuestion = Some(true),
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(true),
    amount = Some(200.00),
    taxPaidQuestion = Some(true),
    taxPaid = Some(50.00)
  )

  val aMinimalStatePensionViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = None,
    startDateQuestion = None,
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(false),
    amount = None,
    taxPaidQuestion = None,
    taxPaid = None
  )

  val aMinimalStatePensionLumpSumViewModel: StateBenefitViewModel = StateBenefitViewModel(
    benefitId = None,
    startDate = None,
    startDateQuestion = None,
    dateIgnoredQuestion = None,
    dateIgnored = None,
    submittedOnQuestion = None,
    submittedOn = None,
    endDateQuestion = None,
    endDate = None,
    amountPaidQuestion = Some(false),
    amount = None,
    taxPaidQuestion = None,
    taxPaid = None
  )

  val anEmptyStateBenefitViewModel: StateBenefitViewModel = StateBenefitViewModel()

}
