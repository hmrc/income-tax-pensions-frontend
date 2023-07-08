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

package models.pension.charges

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import models.pension.income.ForeignPension
import support.UnitTest

class IncomeFromOverseasPensionsViewModelSpec extends UnitTest {

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        anIncomeFromOverseasPensionsViewModel.isFinished
      }
      "all required questions are answered" in {
        anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).isFinished shouldBe true
        anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          PensionScheme(
            alphaThreeCode = None,
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(false),
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = None
          )
        )).isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(true)).isFinished shouldBe false
        anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          PensionScheme(
            alphaThreeCode = None,
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = None
          )
        )).isFinished shouldBe false
      }
    }
  }

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      anIncomeFromOverseasPensionsEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      anIncomeFromOverseasPensionsViewModel.isEmpty shouldBe false
      anIncomeFromOverseasPensionsViewModel.copy(paymentsFromOverseasPensionsQuestion = None).isEmpty shouldBe false
      anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).isEmpty shouldBe false
    }
  }

  "journeyIsNo" should {
    "return true when shortServiceRefund is 'false' and no others have been answered" in {
      anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      anIncomeFromOverseasPensionsEmptyViewModel.journeyIsNo shouldBe false
      anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(true)).journeyIsNo shouldBe false
      anIncomeFromOverseasPensionsViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).journeyIsNo shouldBe false
    }
  }

  "journeyIsUnanswered" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      anIncomeFromOverseasPensionsEmptyViewModel.journeyIsUnanswered
    }
    "return false when any of the ViewModel's arguments are filled" in {
      anIncomeFromOverseasPensionsViewModel.journeyIsUnanswered shouldBe false
      anIncomeFromOverseasPensionsViewModel.copy(paymentsFromOverseasPensionsQuestion = None).journeyIsUnanswered shouldBe false
      anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).journeyIsUnanswered shouldBe false
    }
  }

  "hasPriorData" should {
    "return true when payments question is 'true' and schemes exist" in {
      anIncomeFromOverseasPensionsViewModel.hasPriorData
    }
    "return false when payments question is 'false' and/or no schemes exist" in {
      anIncomeFromOverseasPensionsEmptyViewModel.hasPriorData shouldBe false
      anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)).hasPriorData shouldBe false
      anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq.empty).hasPriorData shouldBe false
    }
  }

  "toForeignPension" should {
    "transform an IncomeFromOverseasPensionsViewModel into Seq[ForeignPension]" in {
      val expectedResult: Seq[ForeignPension] = Seq(
        ForeignPension(
          countryCode = "FRA",
          taxableAmount = 1999.99,
          amountBeforeTax = Some(1999.99),
          taxTakenOff = Some(1999.99),
          specialWithholdingTax = Some(1999.99),
          foreignTaxCreditRelief = Some(true)
        ),
        ForeignPension(
          countryCode = "DEU",
          taxableAmount = 2000.00,
          amountBeforeTax = Some(2000.00),
          taxTakenOff = Some(400.00),
          specialWithholdingTax = Some(400.00),
          foreignTaxCreditRelief = Some(true)
        )
      )

      anIncomeFromOverseasPensionsViewModel.toForeignPension shouldBe expectedResult
    }
    "be empty when there are no OverseasIncomePensionSchemes" in {
      val viewModel: IncomeFromOverseasPensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(true))
      val expectedResult: Seq[ForeignPension] = Seq.empty

      viewModel.toForeignPension shouldBe expectedResult
    }
  }

}
