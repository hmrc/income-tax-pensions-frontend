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

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockPensionUserDataRepository, MockPensionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated}
import models.pension.charges.UnauthorisedPaymentsViewModel
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class PensionsChargesServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockPensionsConnector
  with ScalaFutures {

  val pensionChargesService = new PensionChargesService(mockPensionUserDataRepository, mockPensionsConnector)

  ".saveUnauthorisedViewModel" should {
    "return Right(Unit) when model is saved successfully and unauthorised cya is cleared from DB" in {

      mockFind(taxYear, aUser, Right(Option(aPensionsUserData)))
      val model = aPensionsUserData.pensions.unauthorisedPayments.toCreatePensionChargeRequest
      val userWithEmptyUnauthorisedCya = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel()))
      mockSavePensionChargesSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptyUnauthorisedCya, Right(()))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Right(())
    }
    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(aPensionsUserData)))
      val model = aPensionsUserData.pensions.unauthorisedPayments.toCreatePensionChargeRequest
      mockSavePensionChargesSessionData(nino, taxYear, model, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }
    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(aPensionsUserData)))
      val model = aPensionsUserData.pensions.unauthorisedPayments.toCreatePensionChargeRequest
      val userWithEmptyUnauthorisedCya = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel()))
      mockSavePensionChargesSessionData(nino, taxYear, model, Right(()))
      mockCreateOrUpdate(userWithEmptyUnauthorisedCya, Left(DataNotUpdated))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }


  }


}
