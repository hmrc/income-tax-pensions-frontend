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
import builders.UnauthorisedPaymentsViewModelBuilder.{completeViewModel_WithZeroValue, neitherClaimViewModelNoPSTR, surchargeOnlyViewModel}
import cats.implicits.{catsSyntaxOptionId, none}
import models.pension.AllPensionsData.{Zero, generateSessionModelFromPrior}
import models.pension.charges.{Charge, PensionSchemeUnauthorisedPayments, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.{PaymentsIntoPensionsViewModel, Reliefs}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class AllPensionsDataSpec extends AnyWordSpecLike with TableDrivenPropertyChecks with Matchers {

  val amount: BigDecimal     = BigDecimal(123.00)
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

    def setPensionReliefs(newPensionReliefs: Reliefs): AllPensionsData =
      priorBase.copy(pensionReliefs = priorBase.pensionReliefs.map(_.copy(pensionReliefs = newPensionReliefs)))

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
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false)
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(10.0),
          oneOffPensionContributionsPaid = None,
          retirementAnnuityPayments = None
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
      )
    )

    // TODO Remove after https://jira.tools.tax.service.gov.uk/browse/SASS-8860
    "convert prior data to FE model" ignore forAll(cases) { (downstreamModel, expectedModel) =>
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

  "generateUnauthorisedPaymentsCyaModelFromPrior" when {
    "no prior data exists for unauthorised payments" should {
      "generate an empty session model" in new UnauthorisedPaymentsTest {
        val result = generateSessionModelFromPrior(buildPrior(None)).unauthorisedPayments

        result shouldBe UnauthorisedPaymentsViewModel()
      }
    }
    "an claim has one non-zero amount present" should {
      "populate the session with that claim (i.e. including the 0 value)" in new UnauthorisedPaymentsTest {
        val priorPayments: PensionSchemeUnauthorisedPayments =
          PensionSchemeUnauthorisedPayments(
            pensionSchemeTaxReference = List("some_pstr").some,
            surcharge = Charge(amount, zeroAmount).some,
            noSurcharge = Charge(amount, amount).some
          )

        val result = generateSessionModelFromPrior(buildPrior(priorPayments.some)).unauthorisedPayments

        result shouldBe completeViewModel_WithZeroValue
      }
    }
    "a claim has solely zero amounts" should {
      "retract that claim" in new UnauthorisedPaymentsTest {
        val priorPayments: PensionSchemeUnauthorisedPayments =
          PensionSchemeUnauthorisedPayments(
            pensionSchemeTaxReference = none[List[String]],
            surcharge = Charge(zeroAmount, zeroAmount).some,
            noSurcharge = Charge(zeroAmount, zeroAmount).some
          )

        val result = generateSessionModelFromPrior(buildPrior(priorPayments.some)).unauthorisedPayments

        result shouldBe neitherClaimViewModelNoPSTR
      }
    }
    "handle both claims independently" in new UnauthorisedPaymentsTest {
      val priorPayments: PensionSchemeUnauthorisedPayments =
        PensionSchemeUnauthorisedPayments(
          pensionSchemeTaxReference = List("some_pstr").some,
          surcharge = Charge(amount, amount).some,
          noSurcharge = Charge(zeroAmount, zeroAmount).some
        )

      val result = generateSessionModelFromPrior(buildPrior(priorPayments.some)).unauthorisedPayments

      result shouldBe surchargeOnlyViewModel
    }
  }

  trait UnauthorisedPaymentsTest {
    def buildPrior(unauth: Option[PensionSchemeUnauthorisedPayments]): AllPensionsData =
      priorBase.copy(pensionCharges = priorBase.pensionCharges.map(_.copy(pensionSchemeUnauthorisedPayments = unauth)))
  }
}
