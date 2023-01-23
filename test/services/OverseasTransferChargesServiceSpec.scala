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
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
import models.mongo.DataNotUpdated
import support.mocks.MockPensionSessionService
import support.{ControllerUnitTest, UnitTest}

class OverseasTransferChargesServiceSpec extends UnitTest with MockPensionSessionService with ControllerUnitTest {

  private val underTest = new OverseasTransferChargesService(mockPensionSessionService)

  ".updateOverseasTransferChargeQuestion" should {
    "update cya with transfersIntoOverseasPensions and set whole view model to None when transfersIntoOverseasPensions is false" in {
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel)
      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions =
        emptyTransfersIntoOverseasPensionsViewModel.copy(transfersIntoOverseas = Some(false)))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, question = false)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with transfersIntoOverseasPensions and view model unchanged when transfersIntoOverseasPensions is true" in {

      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel)
      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions =
        aTransfersIntoOverseasPensionsViewModel.copy(transfersIntoOverseas = Some(true)))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, question = true)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "return Left when createOrUpdate fails" in {
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel)
      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions =
        aTransfersIntoOverseasPensionsViewModel.copy(transfersIntoOverseas = Some(true)))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA), Left(DataNotUpdated))

      await(underTest.updateOverseasTransferChargeQuestion(userData, question = true)) shouldBe Left(())
    }
  }
}
