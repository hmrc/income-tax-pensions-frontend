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

import builders.PensionChargesBuilder.anPensionCharges
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import config.MockPensionsConnector
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import utils.UnitTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PensionChargesConnectorHelperSpec extends UnitTest with MockPensionsConnector {

  val pensionChargerConnectorHelper = new PensionChargesConnectorHelper(mockPensionsConnector)
  val model = CreateUpdatePensionChargesRequestModel(
    anPensionCharges.pensionSavingsTaxCharges,
    anPensionCharges.pensionSchemeOverseasTransfers,
    anPensionCharges.pensionSchemeUnauthorisedPayments,
    anPensionCharges.pensionContributions,
    anPensionCharges.overseasPensionContributions,
  )

  ".sendDownstream" should {

    "if the CYA page is no" should {

      val noCYA = anUnauthorisedPaymentsEmptyViewModel.copy(
        surchargeQuestion = Some(false),
        noSurchargeQuestion = Some(false)
      )

      val unauthModel = noCYA.toUnauth

      "save" should {
        "when all sub models are non empty and unauthorised model is empty " in {
          val chargesModel = model.copy(
            pensionSchemeUnauthorisedPayments = None,
          )
          mockSavePensionChargesSessionData(nino, taxYear, chargesModel, Right(()))

          val result = Await.result(pensionChargerConnectorHelper
            .sendDownstream(nino, taxYear, Some(unauthModel), Some(noCYA), chargesModel), Duration.Inf)

          result shouldBe Right(())

        }
      }

      "delete" should {
        "when all sub models are empty, and unauthorised model is empty" in {

          val chargesModel = model.copy(
            pensionSavingsTaxCharges = None,
            pensionContributions = None,
            pensionSchemeOverseasTransfers = None,
            pensionSchemeUnauthorisedPayments = None,
            overseasPensionContributions = None
          )

          mockDeletePensionChargesSessionData(nino, taxYear, Right(()) )

          val result = Await.result(pensionChargerConnectorHelper
            .sendDownstream(nino, taxYear, None, Some(noCYA), chargesModel), Duration.Inf)

          result shouldBe Right(())
        }
        "when all sub models are empty, and unauthorised model is defined but empty" in {

          val chargesModel = model.copy(
            pensionSavingsTaxCharges = None,
            pensionContributions = None,
            pensionSchemeOverseasTransfers = None,
            pensionSchemeUnauthorisedPayments = Some(unauthModel),
            overseasPensionContributions = None
          )

          mockDeletePensionChargesSessionData(nino, taxYear, Right(()) )

          val result = Await.result(pensionChargerConnectorHelper
            .sendDownstream(nino, taxYear, Some(unauthModel), Some(noCYA), chargesModel), Duration.Inf)

          result shouldBe Right(())
        }
      }
    }

    "if the cya page has been answered" in {

      val unauthModel = anUnauthorisedPaymentsViewModel.toUnauth

      val chargesModel = model.copy(
        pensionSavingsTaxCharges = None,
        pensionContributions = None,
        pensionSchemeOverseasTransfers = None,
        pensionSchemeUnauthorisedPayments = Some(unauthModel),
        overseasPensionContributions = None
      )

      mockSavePensionChargesSessionData(nino, taxYear, chargesModel, Right(()) )

      val result = Await.result(pensionChargerConnectorHelper
        .sendDownstream(nino, taxYear, Some(unauthModel), Some(anUnauthorisedPaymentsViewModel), chargesModel), Duration.Inf)

      result shouldBe Right(())
    }

    "if the cya is empty and the models are also empty" in {

      val unauthModel = anUnauthorisedPaymentsEmptyViewModel.toUnauth

      val chargesModel = model.copy(
        pensionSavingsTaxCharges = None,
        pensionContributions = None,
        pensionSchemeOverseasTransfers = None,
        pensionSchemeUnauthorisedPayments = Some(unauthModel),
        overseasPensionContributions = None
      )

      mockDeletePensionChargesSessionData(nino, taxYear, Right(()) )

      val result = Await.result(pensionChargerConnectorHelper
        .sendDownstream(nino, taxYear, Some(unauthModel), Some(anUnauthorisedPaymentsEmptyViewModel), chargesModel), Duration.Inf)

      result shouldBe Right(())

    }
  }

}
