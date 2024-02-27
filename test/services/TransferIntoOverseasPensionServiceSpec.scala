/*
 * Copyright 2024 HM Revenue & Customs
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
import builders.PensionChargesBuilder
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, viewModelNoSchemeDetails}
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, none}
import common.TaxYear
import mocks.{MockPensionConnector, MockSessionRepository, MockSubmissionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData}
import models.pension.charges.{
  CreateUpdatePensionChargesRequestModel,
  PensionCharges,
  PensionSchemeOverseasTransfers,
  TransfersIntoOverseasPensionsViewModel
}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class TransferIntoOverseasPensionServiceSpec extends UnitTest with MockPensionConnector with MockSessionRepository with MockSubmissionsConnector {

  "saving journey answers" when {
    "all external calls are successful" when {
      "providing scheme details" should {
        "save pensions charges" in new Test {
          MockSubmissionsConnector
            .getUserData(nino, taxYear)
            .returns(priorCharges.asRight.asFuture)

          MockSessionRepository
            .find(taxYear, aUser)
            .returns(sessionSchemeDetails.some.asRight.asFuture)

          MockPensionConnector
            .savePensionCharges(nino, taxYear, chargesModelWithSchemeDetails)
            .returns(().asRight.asFuture)

          MockSessionRepository
            .createOrUpdate(clearedJourneyFromSession(sessionSchemeDetails))
            .returns(().asRight.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe ().asRight
        }
      }
      "no scheme details are provided" when {
        "prior charges submissions exists" should {
          "send all prior charges data but omit the 'pensionSchemeOverseasTransfers' json object" in new Test { // so to delete the resource
            MockSubmissionsConnector
              .getUserData(nino, taxYear)
              .returns(priorCharges.asRight.asFuture)

            MockSessionRepository
              .find(taxYear, aUser)
              .returns(sessionNoSchemeDetails.some.asRight.asFuture)

            MockPensionConnector
              .savePensionCharges(nino, taxYear, chargesModelWithJourneyObjectOmitted)
              .returns(().asRight.asFuture)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionNoSchemeDetails))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }
        "no prior charges submissions exist" should {
          "delete pension charges" in new Test {
            MockSubmissionsConnector
              .getUserData(nino, taxYear)
              .returns(noPriorCharges.asRight.asFuture)

            MockSessionRepository
              .find(taxYear, aUser)
              .returns(sessionNoSchemeDetails.some.asRight.asFuture)

            MockPensionConnector
              .deletePensionCharges(nino, taxYear)
              .returns(().asRight.asFuture)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionNoSchemeDetails))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }
      }
    }
    "no user session is found in the database" should {
      "return SessionNotFound" in new Test {
        MockSubmissionsConnector
          .getUserData(nino, taxYear)
          .returns(IncomeTaxUserData(None).asRight.asFuture)

        MockSessionRepository
          .find(taxYear, aUser)
          .returns(DataNotFound.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe DataNotFound.asLeft
      }
    }

    "save pensions charges downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test {
        MockSubmissionsConnector
          .getUserData(nino, taxYear)
          .returns(priorCharges.asRight.asFuture)

        MockSessionRepository
          .find(taxYear, aUser)
          .returns(sessionSchemeDetails.some.asRight.asFuture)

        MockPensionConnector
          .savePensionCharges(nino, taxYear, chargesModelWithSchemeDetails)
          .returns(apiError.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe apiError.asLeft
      }
    }
    "delete pension charges downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test {
        MockSubmissionsConnector
          .getUserData(nino, taxYear)
          .returns(noPriorCharges.asRight.asFuture)

        MockSessionRepository
          .find(taxYear, aUser)
          .returns(sessionNoSchemeDetails.some.asRight.asFuture)

        MockPensionConnector
          .deletePensionCharges(nino, taxYear)
          .returns(apiError.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe apiError.asLeft
      }

      "submissions downstream returns an unsuccessful result" should {
        "return an APIErrorModel" in new Test {
          MockSubmissionsConnector
            .getUserData(nino, taxYear)
            .returns(apiError.asLeft.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe apiError.asLeft
        }
      }

      "session data could not be updated" should {
        "return DataNotUpdated" in new Test {
          MockSubmissionsConnector
            .getUserData(nino, taxYear)
            .returns(noPriorCharges.asRight.asFuture)

          MockSessionRepository
            .find(taxYear, aUser)
            .returns(sessionNoSchemeDetails.some.asRight.asFuture)

          MockPensionConnector
            .deletePensionCharges(nino, taxYear)
            .returns(().asRight.asFuture)

          MockSessionRepository
            .createOrUpdate(clearedJourneyFromSession(sessionNoSchemeDetails))
            .returns(DataNotUpdated.asLeft.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe DataNotUpdated.asLeft
        }
      }
    }
  }

  trait Test {
    def priorWith(charges: Option[PensionCharges]): IncomeTaxUserData =
      IncomeTaxUserData(anAllPensionsData.copy(pensionCharges = charges).some)

    val noPriorCharges = priorWith(none[PensionCharges])
    val priorCharges   = priorWith(PensionChargesBuilder.anPensionCharges.some)

    private def sessionWith(journeyAnswers: TransfersIntoOverseasPensionsViewModel): PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(
          transfersIntoOverseasPensions = journeyAnswers
        ))

    val sessionSchemeDetails   = sessionWith(aTransfersIntoOverseasPensionsViewModel)
    val sessionNoSchemeDetails = sessionWith(viewModelNoSchemeDetails)

    def chargesRequestModel(prior: IncomeTaxUserData, expected: Option[PensionSchemeOverseasTransfers]): CreateUpdatePensionChargesRequestModel =
      CreateUpdatePensionChargesRequestModel
        .fromPriorData(prior)
        .copy(pensionSchemeOverseasTransfers = expected)

    val chargesModelWithSchemeDetails =
      chargesRequestModel(priorCharges, sessionSchemeDetails.pensions.transfersIntoOverseasPensions.maybeToDownstreamRequestModel)

    val chargesModelWithJourneyObjectOmitted =
      chargesRequestModel(priorCharges, none[PensionSchemeOverseasTransfers])

    def clearedJourneyFromSession(session: PensionsUserData): PensionsUserData = {
      val clearedJourneyModel =
        session.pensions.copy(
          transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel.empty
        )
      session.copy(pensions = clearedJourneyModel)
    }

    val apiError: APIErrorModel =
      APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))

    val service = new TransferIntoOverseasPensionService(mockSessionRepository, mockPensionsConnector, mockSubmissionsConnector)

  }

}
