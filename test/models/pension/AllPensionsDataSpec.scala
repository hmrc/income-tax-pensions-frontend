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

package models.pension

import builders.AllPensionsDataBuilder.anAllPensionsData
import cats.implicits.catsSyntaxOptionId
import models.pension.AllPensionsData.Zero
import models.pension.charges.{Charge, PensionSchemeUnauthorisedPayments, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.{PaymentsIntoPensionsViewModel, Reliefs}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class AllPensionsDataSpec extends AnyWordSpecLike with TableDrivenPropertyChecks {

  val amount: BigDecimal = BigDecimal(123.00)
  val zeroAmount: BigDecimal = BigDecimal(0.00)

  val priorBase: AllPensionsData = anAllPensionsData

  "generatePaymentsIntoPensionsCyaFromPrior" should {
    val zero = Zero.some

    val emptyReliefs = Reliefs(None, None, None, None, None)
    val baseModel = PaymentsIntoPensionsViewModel(
      rasPensionPaymentQuestion = Some(false),
      totalRASPaymentsAndTaxRelief = zero,
      oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
      totalOneOffRasPaymentPlusTaxRelief = zero,
      totalPaymentsIntoRASQuestion = Some(true),
      pensionTaxReliefNotClaimedQuestion = Some(false),
      retirementAnnuityContractPaymentsQuestion = Some(false),
      totalRetirementAnnuityContractPayments = zero,
      workplacePensionPaymentsQuestion = Some(false),
      totalWorkplacePensionPayments = zero
    )

    def setPensionReliefs(newPensionReliefs: Reliefs) =
      priorBase.copy(pensionReliefs = priorBase.pensionReliefs.map(_.copy(pensionReliefs = newPensionReliefs)))

    // @formatter:off
    val cases = Table(
      ("Downstream Model", "Expected FE Model"),
      (emptyReliefs, baseModel),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(10.0),
          oneOffPensionContributionsPaid = None
        ),
        baseModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(10.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(10.0),
          oneOffPensionContributionsPaid = None,
          retirementAnnuityPayments = None,
        ),
        baseModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(10.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
          totalPaymentsIntoRASQuestion = Some(true),
          pensionTaxReliefNotClaimedQuestion = Some(false)
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(1.0),
          oneOffPensionContributionsPaid = Some(2.0),
          retirementAnnuityPayments = Some(3.0),
          paymentToEmployersSchemeNoTaxRelief = Some(4.0)
        ),
        baseModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(1.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
          totalOneOffRasPaymentPlusTaxRelief = Some(2.0),
          totalPaymentsIntoRASQuestion = Some(true),
          pensionTaxReliefNotClaimedQuestion = Some(true),
          retirementAnnuityContractPaymentsQuestion = Some(true),
          totalRetirementAnnuityContractPayments = Some(3.0),
          workplacePensionPaymentsQuestion = Some(true),
          totalWorkplacePensionPayments = Some(4.0)
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(1.0),
          oneOffPensionContributionsPaid = Some(2.0),
          retirementAnnuityPayments = Some(3.0),
          paymentToEmployersSchemeNoTaxRelief = None
        ),
        baseModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(1.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
          totalOneOffRasPaymentPlusTaxRelief = Some(2.0),
          totalPaymentsIntoRASQuestion = Some(true),
          pensionTaxReliefNotClaimedQuestion = Some(true),
          retirementAnnuityContractPaymentsQuestion = Some(true),
          totalRetirementAnnuityContractPayments = Some(3.0),
          workplacePensionPaymentsQuestion = Some(false),
          totalWorkplacePensionPayments = zero
        )
      ),
    )
    // @formatter:on

    "convert prior data to FE model" in forAll(cases) { (downstreamModel, expectedModel) =>
      val actual = AllPensionsData.generateSessionModelFromPrior(setPensionReliefs(downstreamModel))
      assert(actual.paymentsIntoPension === expectedModel)
    }
  }

  "getPaymentsIntoPensionsCyaFromPrior" should {
    "return an empty object with preselected totalPaymentsIntoRASQuestion if no prior data for reliefs" in {
      val prior = anAllPensionsData.copy(pensionReliefs = None)
      assert(prior.getPaymentsIntoPensionsCyaFromPrior === PaymentsIntoPensionsViewModel.empty.copy(totalPaymentsIntoRASQuestion = Some(true)))
    }
  }

  "generateUnauthorisedPaymentsCyaModelFromPrior" should {
    def setPriorFromUnauthPayments(journeyPrior: PensionSchemeUnauthorisedPayments): AllPensionsData = {
      val updatedCharges = priorBase.pensionCharges.map(_.copy(pensionSchemeUnauthorisedPayments = journeyPrior.some))

      priorBase.copy(pensionCharges = updatedCharges)
    }

    val sessionBaseModel =
      UnauthorisedPaymentsViewModel(
        surchargeQuestion = true.some,
        noSurchargeQuestion = true.some,
        surchargeAmount = amount.some,
        surchargeTaxAmountQuestion = true.some,
        surchargeTaxAmount = amount.some,
        noSurchargeAmount = amount.some,
        noSurchargeTaxAmountQuestion = true.some,
        noSurchargeTaxAmount = amount.some,
        ukPensionSchemesQuestion = true.some,
        pensionSchemeTaxReference = List("12345678RA").some
      )

    val priorBaseModel =
      PensionSchemeUnauthorisedPayments(
        pensionSchemeTaxReference = List("12345678RA").some,
        surcharge = Charge(amount, amount).some,
        noSurcharge = Charge(amount, amount).some
      )

    val tableCases = Table(
      ("Prior data journey model", "Expected FE model"),
      (priorBaseModel, sessionBaseModel),
      (
        priorBaseModel.copy(surcharge = None),
        sessionBaseModel.copy(
          surchargeQuestion = None,
          surchargeAmount = None,
          surchargeTaxAmountQuestion = None,
          surchargeTaxAmount = None
        )
      ),
      (
        priorBaseModel.copy(surcharge = Charge(zeroAmount, zeroAmount).some),
        sessionBaseModel.copy(
          surchargeQuestion = true.some,
          surchargeAmount = zeroAmount.some,
          surchargeTaxAmountQuestion = true.some,
          surchargeTaxAmount = zeroAmount.some
        )

      ),
      (
        priorBaseModel.copy(noSurcharge = None),
        sessionBaseModel.copy(
          noSurchargeQuestion = None,
          noSurchargeAmount = None,
          noSurchargeTaxAmountQuestion = None,
          noSurchargeTaxAmount = None
        )
      ),
      (
        priorBaseModel.copy(noSurcharge = Charge(zeroAmount, zeroAmount).some),
        sessionBaseModel.copy(
          noSurchargeQuestion = true.some,
          noSurchargeAmount = zeroAmount.some,
          noSurchargeTaxAmountQuestion = true.some,
          noSurchargeTaxAmount = zeroAmount.some
        )
      ),
      (
        priorBaseModel.copy(pensionSchemeTaxReference = None),
        sessionBaseModel.copy(
          ukPensionSchemesQuestion = false.some,
          pensionSchemeTaxReference = None
        )
      )
    )

    "convert prior data to FE model" in forAll(tableCases) { case (priorJourneyModel, expectedModel) =>
      val allPriorData = setPriorFromUnauthPayments(priorJourneyModel)

      val result = AllPensionsData.generateSessionModelFromPrior(allPriorData)
      assert(result.unauthorisedPayments === expectedModel)
    }
  }
}
