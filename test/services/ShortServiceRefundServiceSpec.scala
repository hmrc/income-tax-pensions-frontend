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
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import models.pension.charges.OverseasRefundPensionScheme
import support.mocks.MockPensionSessionService
import support.{ControllerUnitTest, UnitTest}

class ShortServiceRefundServiceSpec extends UnitTest with MockPensionSessionService with ControllerUnitTest {

  private val underTest = new ShortServiceRefundsService(mockPensionSessionService)

  "createOrUpdateShortServiceRefundQuestion." should {
    "update cya with ukRefundCharge to false for ShortServiceRefunds with index 0" in {
      val index = Some(0)
        val refundSchemes = aShortServiceRefundsViewModel.refundPensionScheme.head.copy(ukRefundCharge = None)
        val refundsCya = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(refundSchemes))
        val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundsCya))
        val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
          aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(refundSchemes.copy(ukRefundCharge = Some(false)))))


      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = false, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukRefundCharge to true when no ShortServiceRefunds exists" in {
      val index = None
      val refundsCya = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq.empty)
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundsCya))

      val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
        aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(OverseasRefundPensionScheme(ukRefundCharge = Some(true)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = true, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukRefundCharge to false when no ShortServiceRefunds exists" in {
      val index = None
      val refundsCya = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq.empty)
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundsCya))

      val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
        aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(OverseasRefundPensionScheme(ukRefundCharge = Some(false)))))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = false, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }


    "update cya with ukRefundCharge to true when refundPensionSchemes exists" in {
      val index = None
      val refundSchemes = aShortServiceRefundsViewModel.refundPensionScheme.head.copy(ukRefundCharge = Some(false))
      val refundsCya = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(refundSchemes))
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundsCya))

      val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
        aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(
          refundSchemes.copy(ukRefundCharge = Some(false)), OverseasRefundPensionScheme(ukRefundCharge = Some(true)))
        ))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = true, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with ukRefundCharge to false when refundPensionSchemes exists" in {
      val index = None
      val refundSchemes = aShortServiceRefundsViewModel.refundPensionScheme.head.copy(ukRefundCharge = Some(false))
      val refundsCya = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(refundSchemes))
      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundsCya))

      val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
        aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(
          refundSchemes.copy(ukRefundCharge = Some(false)), OverseasRefundPensionScheme(ukRefundCharge = Some(false)))
        ))

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))

      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = false, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }

    "update cya with existing ukRefundCharge to true when multiple refundPensionSchemes exists but latest scheme does not contain name" in {
      val index = None
      val refundPensionSchemes = Seq(
        OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Scheme 1")),
        OverseasRefundPensionScheme(ukRefundCharge = Some(false), name = None)
      )
      val refundSchemes = aShortServiceRefundsViewModel.copy(refundPensionScheme = refundPensionSchemes)

      val userData = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundSchemes))

      val expectedCYA = aPensionsUserData.pensions.copy(shortServiceRefunds =
        aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(
          OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = Some("Scheme 1")),
          OverseasRefundPensionScheme(ukRefundCharge = Some(true), name = None)
        ))
      )

      mockCreateOrUpdateSessionData(userData.copy(pensions = expectedCYA))
      expectedCYA.shortServiceRefunds.refundPensionScheme.size shouldBe 2
      await(underTest.createOrUpdateShortServiceRefundQuestion(userData, question = true, index)) shouldBe Right(aPensionsUserData.copy(pensions = expectedCYA))
    }
  }
}
