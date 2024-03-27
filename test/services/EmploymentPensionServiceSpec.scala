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
import builders.EmploymentPensionModelBuilder.anotherEmploymentPensionModel
import builders.EmploymentPensionsBuilder
import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, viewModelSingularClaim}
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, none}
import common.TaxYear
import mocks.{MockEmploymentConnector, MockSessionRepository, MockSessionService, MockSubmissionsConnector}
import models.IncomeTaxUserData
import models.IncomeTaxUserData.PriorData
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
    with MockEmploymentConnector
    with BaseServiceSpec {

  "saving journey answers" when {
    "all external calls are successful" when {
      "claiming employment" when {
        "there's no intent to delete any existing employments" should {
          "save employment only" in new Test {
            val idOfSessionClaim = anUkPensionIncomeViewModelOne.employmentId.value

            // I.e. session claims 1 employments, but prior has 0.
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYear))
              .returns((noPriorClaim, sessionOneClaim).asRight.toEitherT)

            MockEmploymentConnector
              .saveEmployment(nino, taxYear)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idOfSessionClaim)
              .never()

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYear)
              .returns(().asRight.asFuture)
              .repeat(1)

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
              .loadPriorAndSession(aUser, TaxYear(taxYear))
              .returns((priorTwoClaims, sessionOneClaim).asRight.toEitherT)

            MockEmploymentConnector
              .saveEmployment(nino, taxYear)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idToDelete)
              .returns(().asRight.asFuture)

            MockEmploymentConnector
              .deleteEmployment(nino, taxYear, idNotToDelete)
              .never()

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYear)
              .returns(().asRight.asFuture)
              .repeat(2)

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
            .loadPriorAndSession(aUser, TaxYear(taxYear))
            .returns((noPriorClaim, sessionNoClaim).asRight.toEitherT)

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
        .loadPriorAndSession(aUser, TaxYear(taxYear))
        .returns(notFoundResponse)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotFound.asLeft
    }
  }

  "save employment downstream returns an unsuccessful result" should {
    "return an APIErrorModel" in new Test {
      MockSessionService
        .loadPriorAndSession(aUser, TaxYear(taxYear))
        .returns((noPriorClaim, sessionOneClaim).asRight.toEitherT)

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
        .loadPriorAndSession(aUser, TaxYear(taxYear))
        .returns((priorTwoClaims, sessionOneClaim).asRight.toEitherT)

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

  "submissions downstream returns an unsuccessful result" should {
    "return an APIErrorModel" in new Test {
      MockSessionService
        .loadPriorAndSession(aUser, TaxYear(taxYear))
        .returns(apiErrorResponse)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe apiError.asLeft
    }
  }

  "session data could not be updated" should {
    "return DataNotUpdated" in new Test {
      // I.e. session claims 1 employments, but prior has 0.
      MockSessionService
        .loadPriorAndSession(aUser, TaxYear(taxYear))
        .returns((noPriorClaim, sessionOneClaim).asRight.toEitherT)

      MockEmploymentConnector
        .saveEmployment(nino, taxYear)
        .returns(().asRight.asFuture)

      MockSubmissionsConnector
        .refreshPensionsResponse(nino, mtditid, taxYear)
        .returns(().asRight.asFuture)
        .repeat(1)

      MockSessionRepository
        .createOrUpdate(clearedJourneyFromSession(sessionOneClaim))
        .returns(DataNotUpdated.asLeft.asFuture)

      val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

      result shouldBe DataNotUpdated.asLeft

    }
  }

  trait Test {
    def priorWith(employment: Option[EmploymentPensions]): PriorData =
      IncomeTaxUserData(anAllPensionsData.copy(employmentPensions = employment).some)

    val noPriorClaim   = priorWith(none[EmploymentPensions])
    val priorTwoClaims = priorWith(EmploymentPensionsBuilder.anEmploymentPensions.some)

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
      mockSubmissionsConnector
    )

  }
}
