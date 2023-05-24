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
import builders.StateBenefitsUserDataBuilder.{aStatePensionBenefitsUD, aStatePensionLumpSumBenefitsUD}
import builders.UserBuilder.aUser
import config.{MockIncomeTaxUserDataConnector, MockPensionUserDataRepository, MockStateBenefitsConnector}
import models.mongo.{DataNotFound, DataNotUpdated}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class StatePensionServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockStateBenefitsConnector
  with MockIncomeTaxUserDataConnector
  with ScalaFutures {

  val statePensionService = new StatePensionService(mockPensionUserDataRepository, mockStateBenefitsConnector)

  val sessionCya = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions)
  val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)
  val userWithEmptySaveIncomeFromPensionsCya = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)

  ".persistIncomeFromPensionsViewModel" should {

    "return Right(Unit) when StatePension and StatePensionLumpSum models are saved successfully and income from pensions cya is cleared from DB" in {
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      mockSaveClaimData(nino, aStatePensionBenefitsUD, Right(()))
      mockSaveClaimData(nino, aStatePensionLumpSumBenefitsUD, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Right(()))

      val result = await(statePensionService.persistIncomeFromPensionsViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))

      val result = await(statePensionService.persistIncomeFromPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      mockSaveClaimData(nino, aStatePensionBenefitsUD, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = await(statePensionService.persistIncomeFromPensionsViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      mockSaveClaimData(nino, aStatePensionBenefitsUD, Right(()))
      mockSaveClaimData(nino, aStatePensionLumpSumBenefitsUD, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Left(DataNotUpdated))

      val result = await(statePensionService.persistIncomeFromPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
}
