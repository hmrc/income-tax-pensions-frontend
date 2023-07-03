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

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockIncomeTaxUserDataConnector, MockPensionUserDataRepository, MockPensionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated}
import models.pension.income.{CreateUpdatePensionIncomeModel, ForeignPensionContainer, OverseasPensionContributionContainer}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class PensionsIncomeServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockPensionsConnector
  with MockIncomeTaxUserDataConnector
  with ScalaFutures {

  val pensionIncomeService = new PensionIncomeService(mockPensionUserDataRepository, mockPensionIncomeConnectorHelper, mockUserDataConnector)

  ".saveIncomeFromOverseasPensionsViewModel" should {
    "return Right(Unit) when model is saved successfully and income income from overseas pensions cya is cleared from DB" in {
      val allPensionsData = anAllPensionsData
      val sessionCya = aPensionsCYAEmptyModel.copy(incomeFromOverseasPensions = aPensionsUserData.pensions.incomeFromOverseasPensions)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      val priorUserData = IncomeTaxUserData(Some(allPensionsData))
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))
      mockFind(aUser.nino, taxYear, priorUserData)


      val model = CreateUpdatePensionIncomeModel(
        foreignPension = Some(ForeignPensionContainer(sessionUserData.pensions.incomeFromOverseasPensions.toForeignPension)),
        overseasPensionContribution = priorUserData.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution)).map(OverseasPensionContributionContainer)
      )

      val userWithEmptySaveIncomeFromOverseasCya = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)
      mockSavePensionIncomeSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromOverseasCya, Right(()))

      val result = await(pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Right(())
    }
    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      val result = await(pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      val allPensionsData = anAllPensionsData
      val sessionCya = aPensionsCYAEmptyModel.copy(incomeFromOverseasPensions = aPensionsUserData.pensions.incomeFromOverseasPensions)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      val priorUserData = IncomeTaxUserData(Some(allPensionsData))
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))
      mockFind(aUser.nino, taxYear, priorUserData)


      val model = CreateUpdatePensionIncomeModel(
        foreignPension = Some(ForeignPensionContainer(sessionUserData.pensions.incomeFromOverseasPensions.toForeignPension)),
        overseasPensionContribution =
          priorUserData.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution)).map(OverseasPensionContributionContainer)
      )

      mockSavePensionIncomeSessionData(nino, taxYear, model, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = await(pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }


    "return Left(DataNotUpdated) when data could not be updated" in {
      val allPensionsData = anAllPensionsData
      val sessionCya = aPensionsCYAEmptyModel.copy(incomeFromOverseasPensions = aPensionsUserData.pensions.incomeFromOverseasPensions)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      val priorUserData = IncomeTaxUserData(Some(allPensionsData))
      mockFind(taxYear, aUser, Right(Option(sessionUserData)))
      mockFind(aUser.nino, taxYear, priorUserData)


      val model = CreateUpdatePensionIncomeModel(
        foreignPension = Some(ForeignPensionContainer(sessionUserData.pensions.incomeFromOverseasPensions.toForeignPension)),
        priorUserData.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution)).map(OverseasPensionContributionContainer)
      )

      val userWithEmptySaveIncomeFromOverseasCya = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)
      mockSavePensionIncomeSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromOverseasCya, Left(DataNotUpdated))

      val result = await(pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
}
