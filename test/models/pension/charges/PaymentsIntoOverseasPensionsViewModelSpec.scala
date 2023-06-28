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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.{aPaymentsIntoOverseasPensionsEmptyViewModel, aPaymentsIntoOverseasPensionsViewModel}
import models.pension.income.OverseasPensionContribution
import utils.UnitTest

class PaymentsIntoOverseasPensionsViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      !aPaymentsIntoOverseasPensionsViewModel.isEmpty
      !aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None).isEmpty
      !aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).isEmpty
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPaymentsIntoOverseasPensionsViewModel.isFinished
      }
      "all required questions are answered" in {
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).isFinished
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(10),
          employerPaymentsQuestion = Some(false)
        ).isFinished
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(10),
          employerPaymentsQuestion = Some(true),
          taxPaidOnEmployerPaymentsQuestion = Some(true)
        ).isFinished
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(
          Relief(
            reliefType = Some(TaxReliefQuestion.NoTaxRelief),
            customerReference = None,
            employerPaymentsAmount = None,
            qopsReference = None,
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None,
            doubleTaxationArticle = None,
            doubleTaxationTreaty = None,
            doubleTaxationReliefAmount = None,
            sf74Reference = None)
        )).isFinished
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true)).isFinished
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(
          Relief(
            reliefType = Some(TaxReliefQuestion.DoubleTaxationRelief),
            customerReference = None,
            employerPaymentsAmount = None,
            qopsReference = None,
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None,
            doubleTaxationArticle = None,
            doubleTaxationTreaty = None,
            doubleTaxationReliefAmount = None,
            sf74Reference = None)
        )).isFinished
      }
    }
  }

  "journeyIsNo" should {
    "return true when shortServiceRefund is 'false' and no others have been answered" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.journeyIsNo
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true)).journeyIsNo
      aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsNo
    }
  }

  "journeyIsUnanswered" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.journeyIsUnanswered
    }
    "return false when any of the ViewModel's arguments are filled" in {
      !aPaymentsIntoOverseasPensionsViewModel.journeyIsUnanswered
      !aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsQuestions = None).journeyIsUnanswered
      !aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsUnanswered
    }
  }

  "toPensionContributions" should {
    "transform a PaymentsIntoOverseasPensionsViewModel into Seq[OverseasPensionContribution]" in {
      val expectedResult: Seq[OverseasPensionContribution] = Seq(
        OverseasPensionContribution(Some("PENSIONINCOME245"), 1999.99, None, None, None, None, None, Some("SF74-123456")),
        OverseasPensionContribution(Some("PENSIONINCOME356"), 100.0, Some("123456"), None, None, None, None, None))

      aPaymentsIntoOverseasPensionsViewModel.toPensionContributions shouldBe expectedResult
    }
    "be empty when there are no Reliefs" in {
      val viewModel: PaymentsIntoOverseasPensionsViewModel = aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true))
      val expectedResult: Seq[OverseasPensionContribution] = Seq.empty

      viewModel.toPensionContributions shouldBe expectedResult
    }
  }

}


