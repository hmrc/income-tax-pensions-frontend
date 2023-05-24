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

import builders.PensionsUserDataBuilder.aPensionsUserData
import models.pension.statebenefits.ClaimCYAModel

import java.time.LocalDate

object ClaimCYAModelBuilder {

  val statePensionData = aPensionsUserData.pensions.incomeFromPensions.statePension
  val statePensionLumpSumData = aPensionsUserData.pensions.incomeFromPensions.statePensionLumpSum

  val aStatePensionClaimCYAModel = ClaimCYAModel(
    benefitId = statePensionData.flatMap(_.benefitId),
    startDate = statePensionData.flatMap(_.startDate).getOrElse(LocalDate.now()),
    endDateQuestion = statePensionData.flatMap(_.endDateQuestion),
    endDate = statePensionData.flatMap(_.endDate),
    dateIgnored = statePensionData.flatMap(_.dateIgnored),
    submittedOn = statePensionData.flatMap(_.submittedOn),
    amount = statePensionData.flatMap(_.amount),
    taxPaidQuestion = statePensionData.flatMap(_.taxPaidQuestion),
    taxPaid = statePensionData.flatMap(_.taxPaid)
  )

  val aStatePensionLumpSumClaimCYAModel = ClaimCYAModel(
    benefitId = statePensionLumpSumData.flatMap(_.benefitId),
    startDate = statePensionLumpSumData.flatMap(_.startDate).getOrElse(LocalDate.now()),
    endDateQuestion = statePensionLumpSumData.flatMap(_.endDateQuestion),
    endDate = statePensionLumpSumData.flatMap(_.endDate),
    dateIgnored = statePensionLumpSumData.flatMap(_.dateIgnored),
    submittedOn = statePensionLumpSumData.flatMap(_.submittedOn),
    amount = statePensionLumpSumData.flatMap(_.amount),
    taxPaidQuestion = statePensionLumpSumData.flatMap(_.taxPaidQuestion),
    taxPaid = statePensionLumpSumData.flatMap(_.taxPaid)
  )

}
