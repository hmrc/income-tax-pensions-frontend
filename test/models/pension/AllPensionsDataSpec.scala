package models.pension

import builders.AllPensionsDataBuilder.anAllPensionsData
import models.pension.AllPensionsData.generatePaymentsIntoPensionsCyaFromPrior
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
