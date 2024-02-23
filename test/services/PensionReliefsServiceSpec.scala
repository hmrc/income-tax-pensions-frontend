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

import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockPensionUserDataRepository, MockPensionsConnector}
import models.error.ApiError.CreateOrUpdateError
import models.mongo.{DataNotFound, DataNotUpdated}
import models.pension.reliefs.{CreateUpdatePensionReliefsModel, PaymentsIntoPensionsViewModel, Reliefs}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.BAD_REQUEST
import utils.CommonData._

class PensionReliefsServiceSpec extends AnyWordSpecLike with MockPensionUserDataRepository with MockPensionsConnector with ScalaFutures {

  val pensionReliefsService = new PensionReliefsService(mockPensionUserDataRepository, mockPensionReliefsConnectorHelper)

  "persistPaymentIntoPensionViewModel" should {
    "return Right when model is saved successfully and payment into pensions cya is cleared from DB" in {
      val sessionCya                              = emptyPensionsData.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData                         = aPensionsUserData.copy(pensions = sessionCya)
      val userWithEmptySavePaymentsIntoPensionCya = aPensionsUserData.copy(pensions = emptyPensionsData)

      mockFind(currTaxYear.endYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )
      mockSavePensionReliefSessionData(nino, currTaxYear.endYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptySavePaymentsIntoPensionCya, Right(()))

      val result = pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, currTaxYear, viewModel, None).value.futureValue
      assert(result === Right(()))
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(currTaxYear.endYear, aUser, Left(DataNotFound))
      val result =
        pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, currTaxYear, PaymentsIntoPensionsViewModel(), None).value.futureValue

      assert(result === Left(CreateOrUpdateError("DataNotFound")))
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      val sessionCya      = emptyPensionsData.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      mockFind(currTaxYear.endYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )

      mockSavePensionReliefSessionData(nino, currTaxYear.endYear, model, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, currTaxYear, viewModel, None).value.futureValue
      assert(result === Left(CreateOrUpdateError("APIErrorModel(400,APIErrorBodyModel(FAILED,failed))")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      val sessionCya                              = emptyPensionsData.copy(paymentsIntoPension = aPensionsUserData.pensions.paymentsIntoPension)
      val sessionUserData                         = aPensionsUserData.copy(pensions = sessionCya)
      val userWithEmptySavePaymentsIntoPensionCya = aPensionsUserData.copy(pensions = emptyPensionsData)

      mockFind(currTaxYear.endYear, aUser, Right(Option(sessionUserData)))

      val viewModel = sessionUserData.pensions.paymentsIntoPension
      val model = CreateUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModel.totalRASPaymentsAndTaxRelief,
          oneOffPensionContributionsPaid = viewModel.totalOneOffRasPaymentPlusTaxRelief,
          retirementAnnuityPayments = viewModel.totalRetirementAnnuityContractPayments,
          paymentToEmployersSchemeNoTaxRelief = viewModel.totalWorkplacePensionPayments,
          overseasPensionSchemeContributions = None
        )
      )
      mockSavePensionReliefSessionData(nino, currTaxYear.endYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptySavePaymentsIntoPensionCya, Left(DataNotUpdated))

      val result = pensionReliefsService.persistPaymentIntoPensionViewModel(aUser, currTaxYear, viewModel, None).value.futureValue
      assert(result === Left(CreateOrUpdateError("DataNotUpdated")))
    }
  }
}
