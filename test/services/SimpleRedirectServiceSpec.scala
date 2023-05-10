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

package services

import controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.mvc.Results.Redirect
import services.SimpleRedirectService.PaymentsIntoPensionsRedirects
import utils.PaymentsIntoPensionPages._
import utils.UnitTest

class SimpleRedirectServiceSpec extends UnitTest {

  val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  val leftRedirect = Left(Redirect(ReliefAtSourcePensionsController.show(taxYear)))

  "PaymentsIntoPensionsRedirects.journeyCheck" should {
    "return Right() if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionViewModel(
            gateway = Some(true),
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
        val result = PaymentsIntoPensionsRedirects.journeyCheck(RetirementAnnuityAmountPage, pIPData, taxYear)

        result shouldBe Right((): Unit)
      }
      "current page is pre-filled and mid-journey" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionViewModel(
            gateway = Some(true),
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
        val result = PaymentsIntoPensionsRedirects.journeyCheck(TaxReliefNotClaimedPage, pIPData, taxYear)

        result shouldBe Right((): Unit)
      }
      "previous page is invalid/unanswered but previous valid question has been answered" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionViewModel(
            gateway = Some(true),
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
        val result = PaymentsIntoPensionsRedirects.journeyCheck(TotalRasPage, pIPData, taxYear)

        result shouldBe Right((): Unit)
      }
    }

    "return Left(redirect) with redirect to RAS page" when {
      "previous question is unanswered" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionViewModel(
            gateway = Some(true),
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
        val result = PaymentsIntoPensionsRedirects.journeyCheck(WorkplacePensionPage, pIPData, taxYear)

        result shouldBe leftRedirect
      }
      "current page is invalid in journey" in {
        val pIPData = cyaData.copy(paymentsIntoPension =
          PaymentsIntoPensionViewModel(
            gateway = Some(true),
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
        val result = PaymentsIntoPensionsRedirects.journeyCheck(OneOffRasPage, pIPData, taxYear)

        result shouldBe leftRedirect
      }
    }
  }
}
