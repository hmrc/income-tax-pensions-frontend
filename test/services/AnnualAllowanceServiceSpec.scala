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
import builders.CreateUpdatePensionChargesRequestBuilder.priorPensionChargesData
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import common.TaxYear
import mocks.{MockPensionConnector, MockSessionRepository, MockSubmissionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, PensionAnnualAllowancesViewModel}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class AnnualAllowanceServiceSpec extends UnitTest with MockPensionConnector with MockSessionRepository with MockSubmissionsConnector {

  "saving journey answers" should {
    "return Unit when saving is successful" in new Test {
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(priorData.asRight.asFuture)

      MockSessionRepository
        .find(taxYear, aUser)
        .returns(sessionData.some.asRight.asFuture)

      MockPensionConnector
        .savePensionCharges(nino, taxYear, chargesDownstreamRequestModel)
        .returns(().asRight.asFuture)

      MockSessionRepository
        .createOrUpdate(clearedJourneyFromSession(sessionData))
        .returns(().asRight.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe ().asRight
    }

    "return SessionNotFound when no user session is found in the database" in new Test {
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(IncomeTaxUserData(None).asRight.asFuture)

      MockSessionRepository
        .find(taxYear, aUser)
        .returns(DataNotFound.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotFound.asLeft
    }

    "return an APIErrorModel when the pensions downstream returns an unsuccessful result" in new Test {
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(priorData.asRight.asFuture)

      MockSessionRepository
        .find(taxYear, aUser)
        .returns(sessionData.some.asRight.asFuture)

      MockPensionConnector
        .savePensionCharges(nino, taxYear, chargesDownstreamRequestModel)
        .returns(apiError.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }

    "return an APIErrorModel when the submissions downstream returns an unsuccessful result" in new Test {
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(apiError.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }

    "return DataNotUpdated when session data could not be updated" in new Test {
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(priorData.asRight.asFuture)

      MockSessionRepository
        .find(taxYear, aUser)
        .returns(sessionData.some.asRight.asFuture)

      MockPensionConnector
        .savePensionCharges(nino, taxYear, chargesDownstreamRequestModel)
        .returns(().asRight.asFuture)

      MockSessionRepository
        .createOrUpdate(clearedJourneyFromSession(sessionData))
        .returns(DataNotUpdated.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotUpdated.asLeft
    }
  }

  trait Test {
    val priorData: IncomeTaxUserData =
      IncomeTaxUserData(anAllPensionsData.some)

    val sessionData: PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(pensionsAnnualAllowances = aPensionsUserData.pensions.pensionsAnnualAllowances)
      )

    def clearedJourneyFromSession(session: PensionsUserData): PensionsUserData = {
      val clearedJourneyModel =
        session.pensions.copy(
          pensionsAnnualAllowances = PensionAnnualAllowancesViewModel()
        )
      session.copy(pensions = clearedJourneyModel)
    }

    val chargesDownstreamRequestModel: CreateUpdatePensionChargesRequestModel = {
      val annualAllowanceChargesModel =
        sessionData.pensions.pensionsAnnualAllowances.toDownstreamRequestModel(Some(anAllPensionsData))

      CreateUpdatePensionChargesRequestModel(
        pensionSavingsTaxCharges = annualAllowanceChargesModel.pensionSavingsTaxCharges,
        pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
        pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
        pensionContributions = annualAllowanceChargesModel.pensionContributions,
        overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
      )
    }

    val apiError: APIErrorModel =
      APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))

    val service = new AnnualAllowanceService(mockSessionRepository, mockPensionsConnector, mockSubmissionsConnector)
  }

}