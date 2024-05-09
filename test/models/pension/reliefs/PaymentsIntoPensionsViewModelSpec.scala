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

package models.pension.reliefs

import builders.PaymentsIntoPensionVewModelBuilder.{aPaymentsIntoPensionViewModel, aPaymentsIntoPensionsEmptyViewModel}
import cats.implicits._
import models.pension.AllPensionsData.Zero
import org.scalatest.prop.TableDrivenPropertyChecks
import support.UnitTest
import testdata.PaymentsIntoPensionsViewModelTestData

class PaymentsIntoPensionsViewModelSpec extends UnitTest with TableDrivenPropertyChecks {

  "isEmpty" should {
    "return true when no questions have been answered" in {
      assert(aPaymentsIntoPensionsEmptyViewModel.isEmpty)
    }
    "return false when any questions have been answered" in {
      aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true)).isEmpty shouldBe false
      aPaymentsIntoPensionViewModel.isEmpty shouldBe false
    }
  }

  "isFinished" should {
    val cases = Table(
      ("model", "expected"),
      (PaymentsIntoPensionsViewModelTestData.answers, true),
      (PaymentsIntoPensionsViewModel(Some(false), None, None, None, None, Some(false), None, None, None, None), true),
      (PaymentsIntoPensionsViewModel(Some(true), None, None, None, None, Some(false), None, None, None, None), false),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), None, None, None, Some(false), None, None, None, None), false),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), None, None, None, None, None), false),
      // Even though we seem to have all answers here, we did not have "Is this correct" - helper question answered
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, None, Some(true), Some(false), None, None, None), false),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(false), Some(true), Some(false), None, None, None), false),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(false), None, None, None, None), true),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(true), Some(false), None, Some(false), None), true),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(true), Some(false), None, Some(true), None), false),
      (
        PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(true), Some(false), None, Some(true), Some(3.0)),
        true),
      (PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(true), Some(true), None, Some(false), None), false),
      (
        PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), None, Some(true), Some(true), Some(true), Some(2.0), Some(false), None),
        true),
      (PaymentsIntoPensionsViewModel(Some(false), None, None, None, None, Some(true), None, None, None, None), false),
      (PaymentsIntoPensionsViewModel(Some(false), None, None, None, None, Some(true), Some(true), None, None, None), false),
      // It's a bit strange case because user selects Retirement Annuity Yes, but then does not provide any source of it (No for rest of the questions)
      (PaymentsIntoPensionsViewModel(Some(false), None, None, None, None, Some(true), Some(true), None, Some(false), None), false)
    )

    "correctly calculate isFinished" in forAll(cases) { case (model, expected) =>
      assert(model.isFinished == expected)
    }
  }

  "journeyIsNo" should {
    "return true when rasPensionPaymentQuestion is 'false' and no others have been answered" in {
      assert(aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(false)).journeyIsNo)
    }
    "return false in any other case" in {
      aPaymentsIntoPensionsEmptyViewModel.journeyIsNo shouldBe false
      aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true)).journeyIsNo shouldBe false
      aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(false)).journeyIsNo shouldBe false
    }
  }

  "fromSubmittedReliefs" should {
    val emptyReliefs = Reliefs.empty
    val zero         = Zero.some

    val cases = Table(
      ("reliefsPriorData", "expectedModel"),
      (
        Reliefs.empty,
        PaymentsIntoPensionsViewModel(Some(false), zero, Some(false), zero, Some(true), Some(false), Some(false), zero, Some(false), zero)),
      (
        emptyReliefs.copy(regularPensionContributions = Some(1.0)),
        PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(false), zero, Some(true), Some(false), Some(false), zero, Some(false), zero)
      ),
      (
        emptyReliefs.copy(regularPensionContributions = Some(1.0), oneOffPensionContributionsPaid = Some(2.0)),
        PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(true), Some(2.0), Some(true), Some(false), Some(false), zero, Some(false), zero)
      ),
      (
        emptyReliefs.copy(regularPensionContributions = Some(1.0), oneOffPensionContributionsPaid = Some(2.0), retirementAnnuityPayments = Some(3.0)),
        PaymentsIntoPensionsViewModel(Some(true), Some(1.0), Some(true), Some(2.0), Some(true), Some(true), Some(true), Some(3.0), Some(false), zero)
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(1.0),
          oneOffPensionContributionsPaid = Some(2.0),
          retirementAnnuityPayments = Some(3.0),
          paymentToEmployersSchemeNoTaxRelief = Some(4.0)
        ),
        PaymentsIntoPensionsViewModel(
          Some(true),
          Some(1.0),
          Some(true),
          Some(2.0),
          Some(true),
          Some(true),
          Some(true),
          Some(3.0),
          Some(true),
          Some(4.0))
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = None,
          oneOffPensionContributionsPaid = Some(2.0),
          retirementAnnuityPayments = Some(3.0),
          paymentToEmployersSchemeNoTaxRelief = Some(4.0)
        ),
        PaymentsIntoPensionsViewModel(Some(false), zero, Some(true), Some(2.0), Some(true), Some(true), Some(true), Some(3.0), Some(true), Some(4.0))
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = None,
          oneOffPensionContributionsPaid = None,
          retirementAnnuityPayments = Some(3.0),
          paymentToEmployersSchemeNoTaxRelief = Some(4.0)
        ),
        PaymentsIntoPensionsViewModel(Some(false), zero, Some(false), zero, Some(true), Some(true), Some(true), Some(3.0), Some(true), Some(4.0))
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = None,
          oneOffPensionContributionsPaid = None,
          retirementAnnuityPayments = None,
          paymentToEmployersSchemeNoTaxRelief = Some(4.0)
        ),
        PaymentsIntoPensionsViewModel(Some(false), zero, Some(false), zero, Some(true), Some(true), Some(false), zero, Some(true), Some(4.0))
      )
    )

    "convert Reliefs into ViewModel" in forAll(cases) { case (reliefsPriorData, expectedModel) =>
      val actualModel = PaymentsIntoPensionsViewModel.fromSubmittedReliefs(reliefsPriorData)
      assert(actualModel === expectedModel)
    }
  }

  "updateRasPensionPaymentQuestion" should {
    val answers = PaymentsIntoPensionsViewModelTestData.answers

    "update answers on true" in {
      val newAnswers = answers.updateRasPensionPaymentQuestion(true)
      assert(newAnswers == answers)
    }

    "should clear dependent answers on false" in {
      val newAnswers = answers.updateRasPensionPaymentQuestion(false)
      assert(
        newAnswers == answers.copy(
          rasPensionPaymentQuestion = Some(false),
          totalRASPaymentsAndTaxRelief = None,
          oneOffRasPaymentPlusTaxReliefQuestion = None,
          totalOneOffRasPaymentPlusTaxRelief = None,
          totalPaymentsIntoRASQuestion = None
        ))
    }
  }

  "updatePensionTaxReliefNotClaimedQuestion" should {
    val answers = PaymentsIntoPensionsViewModelTestData.answers

    "update answers on true" in {
      val newAnswers = answers.updatePensionTaxReliefNotClaimedQuestion(true)
      assert(newAnswers == answers)
    }

    "should clear dependent answers on false" in {
      val newAnswers = answers.updatePensionTaxReliefNotClaimedQuestion(false)
      assert(
        newAnswers == answers.copy(
          pensionTaxReliefNotClaimedQuestion = Some(false),
          retirementAnnuityContractPaymentsQuestion = None,
          totalRetirementAnnuityContractPayments = None,
          workplacePensionPaymentsQuestion = None,
          totalWorkplacePensionPayments = None
        ))
    }
  }

}
