package models.pension

import builders.AllPensionsDataBuilder.anAllPensionsData
import models.pension.AllPensionsData.generatePaymentsIntoPensionsCyaFromPrior
import models.pension.reliefs.Reliefs
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class AllPensionsDataSpec extends AnyWordSpecLike with TableDrivenPropertyChecks {

  "generatePaymentsIntoPensionsCyaFromPrior" when {
    val priorBase = anAllPensionsData
    val reliefs = Reliefs(
      regularPensionContributions = Some(BigDecimal(1.0)),
      oneOffPensionContributionsPaid = Some(BigDecimal(2.0)),
      retirementAnnuityPayments = Some(BigDecimal(3.0)),
      paymentToEmployersSchemeNoTaxRelief = Some(BigDecimal(4.0)),
      overseasPensionSchemeContributions = Some(BigDecimal(5.0))
    )

    def setPensionReliefs(newPensionReliefs: Reliefs) =
      priorBase.copy(pensionReliefs = priorBase.pensionReliefs.map(_.copy(pensionReliefs = newPensionReliefs)))

    def generateForReliefs(newReliefs: Reliefs) = {
      val prior = setPensionReliefs(newReliefs)
      generatePaymentsIntoPensionsCyaFromPrior(prior)
    }

    "regularPensionContributions" should {
      "convert no value" in {
        val actual = generateForReliefs(reliefs.copy(regularPensionContributions = None))

        assert(actual.rasPensionPaymentQuestion === None)
        assert(actual.totalRASPaymentsAndTaxRelief === None)
        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === None)
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === None)
      }

      "convert zero value" in {
        val actual = generateForReliefs(reliefs.copy(regularPensionContributions = Some(0.0)))

        assert(actual.rasPensionPaymentQuestion === Some(false))
        assert(actual.totalRASPaymentsAndTaxRelief === None)
        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === None)
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === None)
      }

      "convert non zero value" in {
        val actual = generateForReliefs(
          reliefs.copy(
            regularPensionContributions = Some(123.0),
            oneOffPensionContributionsPaid = None
          ))

        assert(actual.rasPensionPaymentQuestion === Some(true))
        assert(actual.totalRASPaymentsAndTaxRelief === Some(BigDecimal(123.0)))
        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === None)
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === None)
      }
    }

    "oneOffPensionContributionsPaid" should {
      "convert no value" in {
        val actual = generateForReliefs(reliefs.copy(oneOffPensionContributionsPaid = None))

        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === None)
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === None)
      }

      "convert zero value" in {
        val actual = generateForReliefs(reliefs.copy(oneOffPensionContributionsPaid = Some(0.0)))

        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === Some(false))
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === None)
      }

      "convert non zero value" in {
        val actual = generateForReliefs(reliefs.copy(oneOffPensionContributionsPaid = Some(123.0)))

        assert(actual.oneOffRasPaymentPlusTaxReliefQuestion === Some(true))
        assert(actual.totalOneOffRasPaymentPlusTaxRelief === Some(BigDecimal(123.0)))
      }
    }

    "retirementAnnuityPayments" should {}

    "paymentToEmployersSchemeNoTaxRelief" should {}

    "overseasPensionSchemeContributions" should {}

  }
}
