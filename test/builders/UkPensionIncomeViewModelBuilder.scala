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

import models.pension.statebenefits.UkPensionIncomeViewModel

object UkPensionIncomeViewModelBuilder {

  val anUkPensionIncomeViewModelOne: UkPensionIncomeViewModel = UkPensionIncomeViewModel(
    employmentId = Some("00000000-0000-1000-8000-000000000001"),
    pensionSchemeName = Some("pension name 1"),
    pensionId = Some("Some customer ref 1"),
    startDate = Some("2019-07-23"),
    endDate = Some("2020-07-24"),
    pensionSchemeRef = Some("666/66666"),
    amount = Some(211.33),
    taxPaid = Some(14.77),
    isCustomerEmploymentData = Some(true)
  )

  val anUkPensionIncomeViewModelTwo: UkPensionIncomeViewModel = UkPensionIncomeViewModel(
    employmentId = Some("00000000-0000-1000-8000-000000000002"),
    pensionSchemeName = Some("pension name 2"),
    pensionId = Some("Some hmrc ref 1"),
    startDate = Some("2019-08-23"),
    endDate = Some("2020-08-24"),
    pensionSchemeRef = Some("777/77777"),
    amount = Some(311.44),
    taxPaid = Some(34.88),
    isCustomerEmploymentData = Some(false)
  )

}
