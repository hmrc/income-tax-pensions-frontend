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

import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockPensionUserDataRepository, MockPensionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated}
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, Reliefs}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class PensionReliefsServiceSpec extends UnitTest with MockPensionUserDataRepository with MockPensionsConnector with ScalaFutures {

  val pensionReliefsService = new PensionReliefsService(mockPensionUserDataRepository, mockPensionReliefsConnectorHelper)

  ".persistPaymentIntoPensionViewModel" should {
    "return Right when model is saved successfully and payment into pensions cya is cleared from DB" in {
      val sessionCya      = aPensionsCYAEmptyModel.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateOrUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )
      mockSavePensionReliefSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(Right(()))

      val result = await(pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      val result = await(pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, taxYear))

      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      val sessionCya      = aPensionsCYAEmptyModel.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateOrUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )

      mockSavePensionReliefSessionData(nino, taxYear, model, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = await(pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      val sessionCya      = aPensionsCYAEmptyModel.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateOrUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )
      mockSavePensionReliefSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(Left(DataNotUpdated))

      val result = await(pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
}
