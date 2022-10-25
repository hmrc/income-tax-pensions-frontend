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

import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PaymentsIntoOverseasPensionsViewModel, PensionSchemeSummary, TaxReliefQuestion}

object IncomeFromOverseasPensionsViewModelBuilder {

  val anIncomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(
    paymentsFromOverseasPensions = Some(true),
    overseasPensionSchemes = Some(Seq(
      PensionSchemeSummary(
        country = Some("FRA"),
        pensionPaymentAmount = Some(1999.99),
        pensionPaymentTaxPaid = Some(1999.99),
        specialWithholdingTaxQuestion = Some(true),
        specialWithholdingTaxAmount = Some(1999.99),
        foreignTaxCredit = Some(false),
        taxableAmount = Some(1999.99)
      )
    ))
  )

  val anIncomeFromOverseasPensionsEmptyViewModel: IncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel()
}
