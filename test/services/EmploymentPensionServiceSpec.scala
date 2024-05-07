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

import builders.EmploymentPensionModelBuilder.anotherEmploymentPensionModel
import builders.EmploymentPensionsBuilder
import builders.EmploymentPensionsBuilder.anEmploymentPensions
import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, viewModelSingularClaim}
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import common.TaxYear
import mocks._
import models.mongo.PensionsUserData.SessionData
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData}
import models.pension.employmentPensions.EmploymentPensions
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import utils.UnitTest

class EmploymentPensionServiceSpec
    extends UnitTest
    with MockSubmissionsConnector
    with MockSessionRepository
    with MockSessionService
    with MockPensionConnector
    with MockEmploymentConnector
    with BaseServiceSpec {

  "loading prior data" should {
    "return a EmploymentPensions model when the connector call succeeds" in new Test {
      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(anEmploymentPensions.asRight.asFuture)

      val result = service.loadPriorEmployment(aUser, TaxYear(taxYear)).value.futureValue

      result shouldBe anEmploymentPensions.asRight

    }
    "return a ServiceError when the connector call fails" in new Test {
      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(apiError.asLeft.asFuture)

      val result = service.loadPriorEmployment(aUser, TaxYear(taxYear)).value.futureValue

      result shouldBe apiError.asLeft
    }
  }

  "saving journey answers" when {
    "all external calls are successful" when {
      "claiming employment" when {
        "there's no intent to delete any existing employments" should {
          "save employment only" in new Test {
            val idOfSessionClaim = anUkPensionIncomeViewModelOne.employmentId.value

            // I.e. session claims 1 employments, but prior has 0.
            MockSessionService
              .loadSession(taxYear, user)
              .returns(sessionOneClaim.some.asRight.asFuture)

            MockPensionConnector
              .loadPriorData(nino, TaxYear(taxYear))
              .returns(noPriorClaim.asRight.asFuture)

            MockEmploymentConnector
              .saveEmployment(nino, taxYear)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idOfSessionClaim)
              .never()

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionOneClaim))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }
        "deletion required" should {
          "save employment and trigger deletion of the relevant employments only" in new Test {
            // As this id is not present in the session but is a prior submission
            val idToDelete    = anotherEmploymentPensionModel.employmentId
            val idNotToDelete = anUkPensionIncomeViewModelOne.employmentId.value

            // I.e. session claims 1 employments, but prior has 2.
            MockSessionService
              .loadSession(taxYear, user)
              .returns(sessionOneClaim.some.asRight.asFuture)

            MockPensionConnector
              .loadPriorData(nino, TaxYear(taxYear))
              .returns(priorTwoClaims.asRight.asFuture)

            MockEmploymentConnector
              .saveEmployment(nino, taxYear)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idToDelete)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idNotToDelete)
              .never()

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionOneClaim))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }
      }
      "not claiming employment and nothing to delete" should {
        "not trigger saving or deletion" in new Test {
          // I.e. session claims 0 employments, prior has 0.
          MockSessionService
            .loadSession(taxYear, user)
            .returns(sessionNoClaim.some.asRight.asFuture)

          MockPensionConnector
            .loadPriorData(nino, TaxYear(taxYear))
            .returns(noPriorClaim.asRight.asFuture)

          MockSessionRepository
            .createOrUpdate(clearedJourneyFromSession(sessionNoClaim))
            .returns(().asRight.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe ().asRight
        }
      }
    }
  }
  "no user session is found in the database" should {
    "return SessionNotFound" in new Test {
      MockSessionService
        .loadSession(taxYear, user)
        .returns(DataNotFound.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotFound.asLeft
    }
  }

  "save employment downstream returns an unsuccessful result" should {
    "return an APIErrorModel" in new Test {
      MockSessionService
        .loadSession(taxYear, user)
        .returns(sessionOneClaim.some.asRight.asFuture)

      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(noPriorClaim.asRight.asFuture)

      MockEmploymentConnector
        .saveEmployment(nino, taxYear)
        .returns(apiError.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }
  }
  "delete employment downstream returns an unsuccessful result" should {
    "return an APIErrorModel" in new Test {
      // As this id is not present in the session but is a prior submission
      val expectedId = anotherEmploymentPensionModel.employmentId

      // I.e. session claims 1 employments, but prior has 2.
      MockSessionService
        .loadSession(taxYear, user)
        .returns(sessionOneClaim.some.asRight.asFuture)

      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(priorTwoClaims.asRight.asFuture)

      MockEmploymentConnector
        .deleteEmployment(nino, taxYear, expectedId)
        .returns(apiError.asLeft.asFuture)

      MockEmploymentConnector
        .saveEmployment(nino, taxYear)
        .never()

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }
  }

  "pensions downstream returns an unsuccessful result" should {
    "return an APIErrorModel" in new Test {
      MockSessionService
        .loadSession(taxYear, user)
        .returns(sessionOneClaim.some.asRight.asFuture)

      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(apiError.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }
  }

  "session data could not be updated" should {
    "return DataNotUpdated" in new Test {
      // I.e. session claims 1 employments, but prior has 0.
      MockSessionService
        .loadSession(taxYear, user)
        .returns(sessionOneClaim.some.asRight.asFuture)

      MockPensionConnector
        .loadPriorData(nino, TaxYear(taxYear))
        .returns(noPriorClaim.asRight.asFuture)

      MockEmploymentConnector
        .saveEmployment(nino, taxYear)
        .returns(().asRight.asFuture)

      MockSessionRepository
        .createOrUpdate(clearedJourneyFromSession(sessionOneClaim))
        .returns(DataNotUpdated.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotUpdated.asLeft

    }
  }

  trait Test {

    val noPriorClaim   = EmploymentPensions.empty
    val priorTwoClaims = EmploymentPensionsBuilder.anEmploymentPensions

    private def sessionWith(journeyAnswers: IncomeFromPensionsViewModel): PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(
          incomeFromPensions = journeyAnswers
        ))

    val sessionOneClaim = sessionWith(viewModelSingularClaim)
    val sessionNoClaim  = sessionWith(anIncomeFromPensionEmptyViewModel)

    def clearedJourneyFromSession(session: SessionData): SessionData = {
      val clearedJourneyModel = session.pensions.incomeFromPensions
        .copy(
          uKPensionIncomes = Seq.empty,
          uKPensionIncomesQuestion = None
        )
      session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))
    }

    val service = new EmploymentPensionService(
      mockSessionService,
      mockSessionRepository,
      mockEmploymentConnector,
      mockPensionsConnector
    )

  }
}
