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
import models.pension.income.OverseasPensionContribution
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import utils.UnitTest

class PaymentsIntoOverseasPensionsViewModelSpec extends UnitTest { // scalastyle:off magic.number

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
        aPaymentsIntoOverseasPensionsEmptyViewModel
          .copy(
            paymentsIntoOverseasPensionsQuestions = Some(true),
            paymentsIntoOverseasPensionsAmount = Some(10),
            employerPaymentsQuestion = Some(false)
          )
          .isFinished shouldBe true
        aPaymentsIntoOverseasPensionsEmptyViewModel
          .copy(
            paymentsIntoOverseasPensionsQuestions = Some(true),
            paymentsIntoOverseasPensionsAmount = Some(10),
            employerPaymentsQuestion = Some(true),
            taxPaidOnEmployerPaymentsQuestion = Some(true)
          )
          .isFinished shouldBe true
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(aNoTaxRelief)).isFinished shouldBe true
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true)).isFinished shouldBe false
        aPaymentsIntoOverseasPensionsEmptyViewModel
          .copy(
            paymentsIntoOverseasPensionsQuestions = Some(true),
            taxPaidOnEmployerPaymentsQuestion = Some(true)
          )
          .isFinished shouldBe false
        aPaymentsIntoOverseasPensionsViewModel
          .copy(reliefs = Seq(
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
              sf74Reference = None
            )
          ))
          .isFinished shouldBe false
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
      val viewModel: PaymentsIntoOverseasPensionsViewModel =
        aPaymentsIntoOverseasPensionsEmptyViewModel.copy(paymentsIntoOverseasPensionsQuestions = Some(true))
      val expectedResult: Seq[OverseasPensionContribution] = Seq.empty

      viewModel.toPensionContributions shouldBe expectedResult
    }
  }

  "Relief.isFinished" should {
    "return true when all relevant questions are answered" in {
      Seq(aTransitionalCorrespondingRelief, aMigrantMemberRelief, aDoubleTaxationRelief, aNoTaxRelief).forall(_.isFinished)
    }
    "return false when all not relevant questions are answered" in {
      List(
        aTransitionalCorrespondingRelief.copy(employerPaymentsAmount = None),
        aNoTaxRelief.copy(reliefType = None),
        aMigrantMemberRelief.copy(customerReference = None),
        aDoubleTaxationRelief.copy(alphaThreeCountryCode = None)
      ).zipWithIndex
        .foreach { case (relief, i) =>
          withClue(s"Relief at index $i: ") {
            relief.isFinished shouldBe false
          }
        }
    }
  }

  "updatePensionTaxReliefNotClaimedQuestion" should {
    val model = PaymentsIntoPensionsViewModel(
      Some(true),
      Some(1.0),
      Some(true),
      Some(2.0),
      Some(true),
      Some(true),
      Some(true),
      Some(3.0),
      Some(true),
      Some(4.0)
    )

    "update value to Some(false) when previous was Some(true)" in {
      assert(
        model.updatePensionTaxReliefNotClaimedQuestion(false) ===
          model.copy(
            pensionTaxReliefNotClaimedQuestion = Some(false),
            retirementAnnuityContractPaymentsQuestion = None,
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None
          )
      )
    }

    "update value to Some(true) when previous was Some(false)" in {
      assert(
        model
          .copy(pensionTaxReliefNotClaimedQuestion = Some(false))
          .updatePensionTaxReliefNotClaimedQuestion(true) ===
          model.copy(
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = None,
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None
          )
      )
    }

    "do nothing when updating to Some(false) wit previous value Some(false)" in {
      assert(
        model
          .copy(pensionTaxReliefNotClaimedQuestion = Some(false))
          .updatePensionTaxReliefNotClaimedQuestion(false) ===
          model.copy(
            pensionTaxReliefNotClaimedQuestion = Some(false)
          )
      )
    }

    "do nothing when updating to Some(true) wit previous value Some(true)" in {
      assert(
        model
          .copy(pensionTaxReliefNotClaimedQuestion = Some(true))
          .updatePensionTaxReliefNotClaimedQuestion(true) ===
          model.copy(
            pensionTaxReliefNotClaimedQuestion = Some(true)
          )
      )
    }

  }
}
