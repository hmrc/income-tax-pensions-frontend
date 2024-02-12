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
import models.pension.reliefs.{PaymentsIntoPensionsViewModel, Reliefs}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class AllPensionsDataSpec extends AnyWordSpecLike with TableDrivenPropertyChecks {

  "generatePaymentsIntoPensionsCyaFromPrior" should {
    val priorBase    = anAllPensionsData
    val emptyReliefs = Reliefs(None, None, None, None, None)
    val emptyModel = PaymentsIntoPensionsViewModel(
      rasPensionPaymentQuestion = None,
      totalRASPaymentsAndTaxRelief = None,
      oneOffRasPaymentPlusTaxReliefQuestion = None,
      totalOneOffRasPaymentPlusTaxRelief = None,
      totalPaymentsIntoRASQuestion = Some(true),
      pensionTaxReliefNotClaimedQuestion = None,
      retirementAnnuityContractPaymentsQuestion = None,
      totalRetirementAnnuityContractPayments = None,
      workplacePensionPaymentsQuestion = None,
      totalWorkplacePensionPayments = None
    )

    def setPensionReliefs(newPensionReliefs: Reliefs) =
      priorBase.copy(pensionReliefs = priorBase.pensionReliefs.map(_.copy(pensionReliefs = newPensionReliefs)))

    // @formatter:off
    val cases = Table(
      ("Downstream Model", "Expected FE Model"),
      (emptyReliefs, emptyModel),
      (
        emptyReliefs.copy(regularPensionContributions = Some(0.0)),
        emptyModel.copy(rasPensionPaymentQuestion = Some(false))
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(0.0),
          retirementAnnuityPayments = Some(0.0),
          paymentToEmployersSchemeNoTaxRelief = Some(0.0)
        ),
        emptyModel.copy(
          rasPensionPaymentQuestion = Some(false),
          pensionTaxReliefNotClaimedQuestion = Some(false)
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(10.0),
          oneOffPensionContributionsPaid = Some(0.0)
        ),
        emptyModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(10.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
        )
      ),
      (
        emptyReliefs.copy(
          regularPensionContributions = Some(10.0),
          oneOffPensionContributionsPaid = Some(0.0),
          retirementAnnuityPayments = Some(0.0),
        ),
        emptyModel.copy(
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
        emptyModel.copy(
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
          paymentToEmployersSchemeNoTaxRelief = Some(0.0)
        ),
        emptyModel.copy(
          rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(1.0),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
          totalOneOffRasPaymentPlusTaxRelief = Some(2.0),
          totalPaymentsIntoRASQuestion = Some(true),
          pensionTaxReliefNotClaimedQuestion = Some(true),
          retirementAnnuityContractPaymentsQuestion = Some(true),
          totalRetirementAnnuityContractPayments = Some(3.0),
          workplacePensionPaymentsQuestion = Some(false),
          totalWorkplacePensionPayments = None
        )
      ),
    )
    // @formatter:on

    "convert prior data to FE model" in forAll(cases) { (downstreamModel, expectedModel) =>
      val actual = AllPensionsData.generateCyaFromPrior(setPensionReliefs(downstreamModel))
      assert(actual.paymentsIntoPension === expectedModel)
    }

  }
}
