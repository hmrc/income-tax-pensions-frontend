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
import builders.TransferPensionSchemeBuilder.aNonUkTransferPensionScheme
import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import models.pension.charges.TransferPensionScheme
import support.mocks.MockPensionSessionService
import support.{ControllerUnitTest, UnitTest}

class OverseasTransferChargesServiceSpec extends UnitTest with MockPensionSessionService with ControllerUnitTest {

  private val underTest = new OverseasTransferChargesService(mockPensionSessionService)

  ".updateOverseasTransferChargeQuestion" should {
    "update cya with ukTransferCharge to false for TransferPensionScheme with index 0" in {
      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
        transferPensionScheme = Seq(TransferPensionScheme(ukTransferCharge = Some(false)), aNonUkTransferPensionScheme)))

      mockCreateOrUpdateSessionData(aPensionsUserData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(aPensionsUserData, yesNo = false, Some(0))) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukTransferCharge to true when no TransferPensionScheme exists" in {
      val index        = None
      val transfersCya = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq.empty)
      val userData     = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transfersCya))

      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions =
        aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(TransferPensionScheme(ukTransferCharge = Some(true)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, yesNo = true, index)) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukTransferCharge to false when no TransferPensionScheme exists" in {
      val index        = None
      val transfersCya = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq.empty)
      val userData     = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transfersCya))

      val expectedCYA = aPensionsUserData.pensions.copy(transfersIntoOverseasPensions =
        aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(TransferPensionScheme(ukTransferCharge = Some(false)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, yesNo = false, index)) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukTransferCharge to true when TransferPensionSchemes exists" in {
      val index           = None
      val transferSchemes = aTransfersIntoOverseasPensionsViewModel.transferPensionScheme.head.copy(ukTransferCharge = Some(false))
      val transfersCya    = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(transferSchemes))
      val userData        = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transfersCya))

      val expectedCYA =
        aPensionsUserData.pensions.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme =
          Seq(transferSchemes.copy(ukTransferCharge = Some(false)), TransferPensionScheme(ukTransferCharge = Some(true)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, yesNo = true, index)) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukTransferCharge to false when TransferPensionSchemes exists" in {
      val index           = None
      val transferSchemes = aTransfersIntoOverseasPensionsViewModel.transferPensionScheme.head.copy(ukTransferCharge = Some(false))
      val transfersCya    = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(transferSchemes))
      val userData        = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transfersCya))

      val expectedCYA =
        aPensionsUserData.pensions.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme =
          Seq(transferSchemes.copy(ukTransferCharge = Some(false)), TransferPensionScheme(ukTransferCharge = Some(false)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.updateOverseasTransferChargeQuestion(userData, yesNo = false, index)) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with new scheme with ukTransferCharge to true when index is none but other TransferPensionSchemes exists" in {
      val index = None
      val transferPensionSchemes = Seq(
        TransferPensionScheme(ukTransferCharge = Some(true), name = Some("Scheme 1")),
        TransferPensionScheme(ukTransferCharge = Some(false))
      )
      val transferSchemes = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = transferPensionSchemes)

      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferSchemes))

      val expectedCYA =
        aPensionsUserData.pensions.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(
          TransferPensionScheme(ukTransferCharge = Some(true), name = Some("Scheme 1")),
          TransferPensionScheme(ukTransferCharge = Some(false)),
          TransferPensionScheme(ukTransferCharge = Some(true))
        )))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))
      expectedCYA.transfersIntoOverseasPensions.transferPensionScheme.size shouldBe 3
      await(underTest.updateOverseasTransferChargeQuestion(userData, yesNo = true, index)) shouldBe Right(
        aPensionsUserData.copy(pensions = expectedCYA))
    }
  }
}
