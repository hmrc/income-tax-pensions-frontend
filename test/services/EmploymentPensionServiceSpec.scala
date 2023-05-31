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
import config.{MockEmploymentConnector, MockPensionUserDataRepository}
import models.mongo.{DataNotFound, DataNotUpdated, DatabaseError, PensionsUserData}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class EmploymentPensionServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockEmploymentConnector
  with ScalaFutures {

  val employmentPensionService = new EmploymentPensionService(mockPensionUserDataRepository, mockEmploymentConnector)

  ".persistUkPensionIncomeViewModel" should {
    
    "return Right(Unit) when model is saved successfully and UK Pensions cya is cleared from DB" in new Setup {
      mockSessionFind()
      mockEmploymentSaves()
      mockUpdateSession()
      val result = await(employmentPensionService.persistUkPensionIncomeViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in new Setup {
      override val findResponse: Either[DatabaseError, Option[PensionsUserData]] = Left(DataNotFound)
      mockSessionFind()
      val result = await(employmentPensionService.persistUkPensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when Bad Request is returned from connector" in new Setup {
      override val saveResponse = Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      mockSessionFind()
      mockEmploymentSaves()
      val result = await(employmentPensionService.persistUkPensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when session data could not be cleared from data base" in new Setup {
      override val updateSessionResponse = Left(DataNotUpdated)
      mockSessionFind()
      mockEmploymentSaves()
      mockUpdateSession()
      val result = await(employmentPensionService.persistUkPensionIncomeViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
    
    trait Setup {
      type SessionResponse = Either[APIErrorModel, Unit]
      
      val findResponse: Either[DatabaseError, Option[PensionsUserData]] = Right(Option(sessionUserData))
      val saveResponse: SessionResponse = Right(())
      val updateSessionResponse: Either[DatabaseError, Unit] = Right(())

      lazy val sessionUserData = aPensionsUserData.copy(
        pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions))

      def mockSessionFind():Unit = mockFind(taxYear, aUser, findResponse)

      def mockEmploymentSaves(): Unit =
        sessionUserData.pensions.incomeFromPensions.uKPensionIncomes.map(_.toCreateUpdateEmploymentRequest)
          .foreach(cuer => mockSaveEmploymentPensionsData(nino, taxYear, cuer, saveResponse))
      
      lazy val userWithEmptySaveIncomeFromPensionCya = aPensionsUserData
        .copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions.copy(uKPensionIncomes = Nil)))
      
      def mockUpdateSession(): Unit =
        mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionCya, updateSessionResponse)
      
    }
  }
  
}
