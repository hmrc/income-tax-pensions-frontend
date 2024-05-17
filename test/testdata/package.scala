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

import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsEmptyViewModel
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionEmptyViewModel
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.ShortServiceRefundsViewModelBuilder.emptyShortServiceRefundsViewModel
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import models.mongo.PensionsCYAModel
import testdata.TransfersIntoOverseasPensionsViewModelTestData.transfersIntoOverseasPensionsAnswers

package object testdata {
  val allData = PensionsCYAModel(
    PaymentsIntoPensionsViewModelTestData.answers,
    AnnualAllowancesViewModelTestData.answers,
    anIncomeFromPensionEmptyViewModel,
    anUnauthorisedPaymentsViewModel,
    aPaymentsIntoOverseasPensionsViewModel,
    anIncomeFromOverseasPensionsEmptyViewModel,
    transfersIntoOverseasPensionsAnswers,
    emptyShortServiceRefundsViewModel
  )
}
