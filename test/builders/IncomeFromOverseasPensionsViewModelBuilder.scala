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

import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}

object IncomeFromOverseasPensionsViewModelBuilder {

  val anIncomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(
    paymentsFromOverseasPensionsQuestion = Some(true),
    overseasIncomePensionSchemes = Seq(
      PensionScheme(
        alphaThreeCode = Some("FRA"),
        alphaTwoCode = Some("FR"),
        pensionPaymentAmount = Some(1999.99),
        pensionPaymentTaxPaid = Some(1999.99),
        specialWithholdingTaxQuestion = Some(true),
        specialWithholdingTaxAmount = Some(1999.99),
        foreignTaxCreditReliefQuestion = Some(true),
        taxableAmount = Some(1999.99)
      ),
      PensionScheme(
        alphaThreeCode = Some("DEU"),
        alphaTwoCode = Some("DE"),
        pensionPaymentAmount = Some(2000.00),
        pensionPaymentTaxPaid = Some(400.00),
        specialWithholdingTaxQuestion = Some(true),
        specialWithholdingTaxAmount = Some(400.00),
        foreignTaxCreditReliefQuestion = Some(true),
        taxableAmount = Some(2000.00)
      )
    )
  )
  
  val anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel: IncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(
    paymentsFromOverseasPensionsQuestion = Some(true),
    overseasIncomePensionSchemes = Seq(
      PensionScheme(
        alphaThreeCode = Some("FRA"),
        alphaTwoCode = Some("FR"),
        pensionPaymentAmount = Some(2999.99),
        pensionPaymentTaxPaid = Some(999.99),
        specialWithholdingTaxQuestion = Some(true),
        specialWithholdingTaxAmount = Some(1999.99),
        foreignTaxCreditReliefQuestion = Some(false),
        taxableAmount = Some(2000.00)
      )
    )
  )
  
  val anIncomeFromOverseasPensionsEmptyViewModel: IncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel()
}
