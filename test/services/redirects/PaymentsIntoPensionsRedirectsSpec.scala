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

package services.redirects

import controllers.pensions.paymentsIntoPensions.routes.{PaymentsIntoPensionsCYAController, ReliefAtSourcePensionsController, TotalPaymentsIntoRASController}
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.mvc.Call
import play.api.mvc.Results.Redirect
import services.redirects.PaymentsIntoPensionPages._
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import utils.UnitTest

class PaymentsIntoPensionsRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val someRedirect = Some(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
  private val cyaRedirect: Call = PaymentsIntoPensionsCYAController.show(taxYear)
  private val contextualRedirect: Call = TotalPaymentsIntoRASController.show(taxYear)

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(true),
            totalRASPaymentsAndTaxRelief = Some(45.54),
            oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
            totalOneOffRasPaymentPlusTaxRelief = Some(64.46),
            totalPaymentsIntoRASQuestion = Some(true),
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = Some(true),
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(RetirementAnnuityAmountPage, pIPData, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(true),
            totalRASPaymentsAndTaxRelief = Some(45.54),
            oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
            totalOneOffRasPaymentPlusTaxRelief = Some(64.46),
            totalPaymentsIntoRASQuestion = Some(true),
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = Some(true),
            totalRetirementAnnuityContractPayments = Some(45),
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(RetirementAnnuityAmountPage, pIPData, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(false),
            totalRASPaymentsAndTaxRelief = None,
            oneOffRasPaymentPlusTaxReliefQuestion = None,
            totalOneOffRasPaymentPlusTaxRelief = None,
            totalPaymentsIntoRASQuestion = None,
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = Some(true),
            totalRetirementAnnuityContractPayments = Some(100.10),
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(TaxReliefNotClaimedPage, pIPData, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(true),
            totalRASPaymentsAndTaxRelief = Some(45.54),
            oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
            totalOneOffRasPaymentPlusTaxRelief = None,
            totalPaymentsIntoRASQuestion = None,
            pensionTaxReliefNotClaimedQuestion = None,
            retirementAnnuityContractPaymentsQuestion = None,
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(TotalRasPage, pIPData, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to RAS page" when {
      "previous question is unanswered" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(true),
            totalRASPaymentsAndTaxRelief = Some(45.54),
            oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
            totalOneOffRasPaymentPlusTaxRelief = Some(64.46),
            totalPaymentsIntoRASQuestion = Some(true),
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = Some(true),
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(WorkplacePensionPage, pIPData, taxYear)

        result shouldBe someRedirect
      }
      "current page is invalid in journey" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionsViewModel(
            rasPensionPaymentQuestion = Some(true),
            totalRASPaymentsAndTaxRelief = None,
            oneOffRasPaymentPlusTaxReliefQuestion = None,
            totalOneOffRasPaymentPlusTaxRelief = None,
            totalPaymentsIntoRASQuestion = None,
            pensionTaxReliefNotClaimedQuestion = Some(true),
            retirementAnnuityContractPaymentsQuestion = Some(true),
            totalRetirementAnnuityContractPayments = None,
            workplacePensionPaymentsQuestion = None,
            totalWorkplacePensionPayments = None)
        )
        val result = journeyCheck(OneOffRasPage, pIPData, taxYear)

        result shouldBe someRedirect
      }
    }
  }

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe PaymentsIntoPensionsCYAController.show(taxYear)
    }
  }
}
