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
import builders.StateBenefitViewModelBuilder.{aPriorStatePensionLumpSumViewModel, aPriorStatePensionViewModel}
import builders.StateBenefitsUserDataBuilder.{
  aCreateStatePensionBenefitsUD,
  aCreateStatePensionLumpSumBenefitsUD,
  anUpdateStatePensionBenefitsUD,
  anUpdateStatePensionLumpSumBenefitsUD
}
import builders.UserBuilder.aUser
import config.{MockIncomeTaxUserDataConnector, MockPensionUserDataRepository, MockStateBenefitsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, PensionsCYAModel, PensionsUserData}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class StatePensionServiceSpec
    extends UnitTest
    with MockPensionUserDataRepository
    with MockStateBenefitsConnector
    with MockIncomeTaxUserDataConnector
    with ScalaFutures {

  val statePensionService = new StatePensionService(mockPensionUserDataRepository, mockStateBenefitsConnector)

  val sessionCya: PensionsCYAModel      = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions)
  val sessionUserData: PensionsUserData = aPensionsUserData.copy(pensions = sessionCya)
  val userWithEmptySaveIncomeFromPensionsCya: PensionsUserData = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)

  ".persistIncomeFromPensionsViewModel" should {

    "return Right(Unit) and clear income from pensions cya from DB" when {

      "both StatePension and StatePensionLumpSum models are successfully submitted" in {
        mockFind(taxYear, aUser, Right(Option(sessionUserData)))

        mockSaveClaimData(nino, aCreateStatePensionBenefitsUD, Right(()))
        mockSaveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD, Right(()))
        mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Right(()))

        val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
        result shouldBe Right(())
      }

      "only StatePension model is submitted, updating a prior submission" in {
        val statePensionOnlySessionData: PensionsUserData =
          sessionUserData.copy(pensions = sessionUserData.pensions.copy(
            incomeFromPensions = sessionCya.incomeFromPensions.copy(statePension = Some(aPriorStatePensionViewModel), statePensionLumpSum = None)
          ))

        mockFind(taxYear, aUser, Right(Option(statePensionOnlySessionData)))

        mockSaveClaimData(nino, anUpdateStatePensionBenefitsUD, Right(()))
        mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Right(()))

        val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
        result shouldBe Right(())
      }

      "only StatePensionLumpSum model is submitted, updating a prior submission" in {
        val statePensionLumpSumOnlySessionData: PensionsUserData =
          sessionUserData.copy(pensions = sessionUserData.pensions.copy(
            incomeFromPensions =
              sessionCya.incomeFromPensions.copy(statePension = None, statePensionLumpSum = Some(aPriorStatePensionLumpSumViewModel))
          ))

        mockFind(taxYear, aUser, Right(Option(statePensionLumpSumOnlySessionData)))

        mockSaveClaimData(nino, anUpdateStatePensionLumpSumBenefitsUD, Right(()))
        mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Right(()))

        val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
        result shouldBe Right(())
      }
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))

      val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      mockSaveClaimData(nino, aCreateStatePensionBenefitsUD, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      mockSaveClaimData(nino, aCreateStatePensionBenefitsUD, Right(()))
      mockSaveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Left(DataNotUpdated))

      val result = await(statePensionService.persistStatePensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
}
