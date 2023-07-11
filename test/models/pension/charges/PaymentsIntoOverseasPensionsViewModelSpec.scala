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
import builders.ReliefBuilder.{aDoubleTaxationRelief, aMigrantMemberRelief, aNoTaxRelief, aTransitionalCorrespondingRelief}
import models.pension.charges.TaxReliefQuestion.{MigrantMemberRelief, TransitionalCorrespondingRelief}
import models.pension.income.OverseasPensionContribution
import utils.UnitTest

class PaymentsIntoOverseasPensionsViewModelSpec extends UnitTest {

  "isEmpty" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.isEmpty
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aPaymentsIntoOverseasPensionsViewModel.isEmpty shouldBe false
      aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None).isEmpty shouldBe false
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).isEmpty shouldBe false
    }
  }

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        aPaymentsIntoOverseasPensionsViewModel.isFinished
      }
      "all required questions are answered" in {
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).isFinished shouldBe true
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(10),
          employerPaymentsQuestion = Some(false)
        ).isFinished shouldBe true
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(10),
          employerPaymentsQuestion = Some(true),
          taxPaidOnEmployerPaymentsQuestion = Some(true)
        ).isFinished shouldBe true
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(aNoTaxRelief)).isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true)).isFinished shouldBe false
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(true), taxPaidOnEmployerPaymentsQuestion = Some(true)
        ).isFinished shouldBe false
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
        )).isFinished shouldBe false
      }
    }
  }

  "journeyIsNo" should {
    "return true when shortServiceRefund is 'false' and no others have been answered" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsNo
    }
    "return false in any other case" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.journeyIsNo shouldBe false
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true)).journeyIsNo shouldBe false
      aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsNo shouldBe false
    }
  }

  "journeyIsUnanswered" should {
    "return true when all the ViewModel's arguments are 'None'" in {
      aPaymentsIntoOverseasPensionsEmptyViewModel.journeyIsUnanswered
    }
    "return false when any of the ViewModel's arguments are filled" in {
      aPaymentsIntoOverseasPensionsViewModel.journeyIsUnanswered shouldBe false
      aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsQuestions = None).journeyIsUnanswered shouldBe false
      aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(false)).journeyIsUnanswered shouldBe false
    }
  }

  "toPensionContributions" should {
    "transform a PaymentsIntoOverseasPensionsViewModel into Seq[OverseasPensionContribution]" in {
      val expectedResult: Seq[OverseasPensionContribution] = Seq(
        OverseasPensionContribution(Some("tcrPENSIONINCOME2000"), 1999.99, None, None, None, None, None, Some("SF74-123456")),
        OverseasPensionContribution(Some("mmrPENSIONINCOME356"), 356.0, Some("123456"), None, None, None, None, None),
        OverseasPensionContribution(Some("dtrPENSIONINCOME550"), 550.0, Some("123456"), Some(55), Some("ATG"), None, None, None),
        OverseasPensionContribution(Some("noPENSIONINCOME100"), 100, None, None, None, None, None, None)
      )

      aPaymentsIntoOverseasPensionsViewModel.toPensionContributions shouldBe expectedResult
    }
    "be empty when there are no Reliefs" in {
      val viewModel: PaymentsIntoOverseasPensionsViewModel = aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true))
      val expectedResult: Seq[OverseasPensionContribution] = Seq.empty

      viewModel.toPensionContributions shouldBe expectedResult
    }
  }

  "Relief.isFinished" should {
    "return true when all relevant questions are answered" in {
      Seq(aTransitionalCorrespondingRelief, aMigrantMemberRelief, aDoubleTaxationRelief, aNoTaxRelief).forall(_.isFinished)
    }
    "return false when all not relevant questions are answered" in {
      aTransitionalCorrespondingRelief.copy(employerPaymentsAmount = None).isFinished shouldBe false
      aMigrantMemberRelief.copy(qopsReference = None).isFinished shouldBe false
      aDoubleTaxationRelief.copy(alphaThreeCountryCode = None).isFinished shouldBe false
      aNoTaxRelief.copy(reliefType = None).isFinished shouldBe false
    }
  }

}


