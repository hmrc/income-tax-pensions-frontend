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
import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, spAndSpLumpSum, statePensionOnly}
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.StateBenefitBuilder.{anStateBenefitFour, anStateBenefitThree}
import builders.StateBenefitViewModelBuilder.{anStateBenefitViewModel, anStateBenefitViewModelOne}
import builders.StateBenefitsModelBuilder
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, none}
import common.TaxYear
import mocks.{MockSessionRepository, MockSessionService, MockStateBenefitsConnector, MockSubmissionsConnector}
import models.IncomeTaxUserData
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData}
import models.pension.statebenefits.{AllStateBenefitsData, IncomeFromPensionsViewModel}
import org.scalatest.OptionValues.convertOptionToValuable
import utils.UnitTest

class StatePensionServiceSpec
    extends UnitTest
    with MockSubmissionsConnector
    with MockSessionRepository
    with MockSessionService
    with MockStateBenefitsConnector
    with BaseServiceSpec {

  "saving journey answers" when {
    "all external calls are successful" when {
      "there's no intent to delete existing claims" when {
        "claiming state pension or state pension lump sum" should {
          "save the one being claimed" in new Test {
            // This is the benefitId being claimed for in the session.
            val benefitIdFromSession = anStateBenefitViewModelOne.benefitId.value

            // I.e. session claims 1 state benefit, but prior has 0.
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
              .returns((noPriorClaim, sessionSpOnly).asRight.toEitherT)

            MockStateBenefitsConnector
              .saveClaim(nino)
              .returns(().asRight.asFuture)

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, benefitIdFromSession)
              .never()

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYearEOY)
              .returns(().asRight.asFuture)
              .repeat(1)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionSpOnly))
              .returns(().asRight.asFuture)

            val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

            result shouldBe ().asRight
          }
        }
        "claiming both state pension lump sum and state pension" should {
          "save both of them" in new Test {
            val spBenefitIdFromSession        = anStateBenefitViewModelOne.benefitId.value
            val spLumpSumBenefitIdFromSession = anStateBenefitViewModel.benefitId.value

            // I.e. session claims 2 state benefit, but prior has 0.
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
              .returns((noPriorClaim, sessionSpAndSpLumpSum).asRight.toEitherT)

            MockStateBenefitsConnector
              .saveClaim(nino)
              .returns(().asRight.asFuture)
              .repeat(2)

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, spBenefitIdFromSession)
              .never()

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, spLumpSumBenefitIdFromSession)
              .never()

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYearEOY)
              .returns(().asRight.asFuture)
              .repeat(2)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionSpAndSpLumpSum))
              .returns(().asRight.asFuture)

            val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

            result shouldBe ().asRight
          }
        }
      }
      "intent is to delete a claim" when {
        "intent is to delete both claims" should {
          "delete both" in new Test {
            // Both the benefitIds that for existing claims, hence ones that should be deleted.
            val priorSpBenefitId        = anStateBenefitThree.benefitId
            val priorSpLumpSumBenefitId = anStateBenefitFour.benefitId

            // I.e. session claims 0 state benefit, but prior has 2.
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
              .returns((priorSpAndSpLumpSum, sessionNoClaim).asRight.toEitherT)

            MockStateBenefitsConnector
              .saveClaim(nino)
              .never()

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, priorSpBenefitId)
              .returns(().asRight.asFuture)

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, priorSpLumpSumBenefitId)
              .returns(().asRight.asFuture)

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYearEOY)
              .returns(().asRight.asFuture)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionNoClaim))
              .returns(().asRight.asFuture)

            val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

            result shouldBe ().asRight
          }
        }
        "intent is to delete one of the claims" should {
          "delete one only" in new Test {
            // The benefitId of the claim that exists as a prior submission but not a claim in the session.
            val priorSpLumpSumBenefitId = anStateBenefitFour.benefitId
            // The session benefitId
            val spBenefitIdFromSession = anStateBenefitViewModelOne.benefitId.value

            // I.e. session claims 1 state benefit, but prior has 2.
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
              .returns((priorSpAndSpLumpSum, sessionSpOnly).asRight.toEitherT)

            MockStateBenefitsConnector
              .saveClaim(nino)
              .returns(().asRight.asFuture)
              .repeat(1)

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, priorSpLumpSumBenefitId)
              .returns(().asRight.asFuture)

            MockStateBenefitsConnector
              .deleteClaim(nino, taxYearEOY, spBenefitIdFromSession)
              .never()

            MockSubmissionsConnector
              .refreshPensionsResponse(nino, mtditid, taxYearEOY)
              .returns(().asRight.asFuture)
              .repeat(2)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionSpOnly))
              .returns(().asRight.asFuture)

            val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

            result shouldBe ().asRight
          }
        }
      }
    }
    "no user session is found in the database" should {
      "return SessionNotFound" in new Test {
        MockSessionService
          .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
          .returns(notFoundResponse)

        val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

        result shouldBe DataNotFound.asLeft
      }
    }

    "save state pension downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test {
        MockSessionService
          .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
          .returns((noPriorClaim, sessionSpOnly).asRight.toEitherT)

        MockStateBenefitsConnector
          .saveClaim(nino)
          .returns(apiError.asLeft.asFuture)

        val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

        result shouldBe apiError.asLeft
      }
    }
    "delete state pensions downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test {
        // The benefitId of the claim that exists as a prior submission but not a claim in the session.
        val priorSpLumpSumBenefitId = anStateBenefitFour.benefitId

        // I.e. session claims 1 state benefit, but prior has 2.
        MockSessionService
          .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
          .returns((priorSpAndSpLumpSum, sessionSpOnly).asRight.toEitherT)

        MockStateBenefitsConnector
          .deleteClaim(nino, taxYearEOY, priorSpLumpSumBenefitId)
          .returns(apiError.asLeft.asFuture)

        val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

        result shouldBe apiError.asLeft
      }
    }

    "submissions downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test {
        MockSessionService
          .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
          .returns(apiErrorResponse)

        val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

        result shouldBe apiError.asLeft
      }
    }

    "session data could not be updated" should {
      "return DataNotUpdated" in new Test {
        // This is the benefitId being claimed for in the session.
        val benefitIdFromSession = anStateBenefitViewModelOne.benefitId.value

        // I.e. session claims 1 state benefit, but prior has 0.
        MockSessionService
          .loadPriorAndSession(aUser, TaxYear(taxYearEOY))
          .returns((noPriorClaim, sessionSpOnly).asRight.toEitherT)

        MockStateBenefitsConnector
          .saveClaim(nino)
          .returns(().asRight.asFuture)

        MockStateBenefitsConnector
          .deleteClaim(nino, taxYearEOY, benefitIdFromSession)
          .never()

        MockSubmissionsConnector
          .refreshPensionsResponse(nino, mtditid, taxYearEOY)
          .returns(().asRight.asFuture)
          .repeat(1)

        MockSessionRepository
          .createOrUpdate(clearedJourneyFromSession(sessionSpOnly))
          .returns(DataNotUpdated.asLeft.asFuture)

        val result = await(service.saveAnswers(aUser, TaxYear(taxYearEOY)))

        result shouldBe DataNotUpdated.asLeft
      }
    }
  }

  trait Test {
    def priorWith(stateBenefits: Option[AllStateBenefitsData]): IncomeTaxUserData =
      IncomeTaxUserData(anAllPensionsData.copy(stateBenefits = stateBenefits).some)

    val noPriorClaim        = priorWith(none[AllStateBenefitsData])
    val priorSpAndSpLumpSum = priorWith(StateBenefitsModelBuilder.aStateBenefitsModel.some)

    def sessionWith(answers: IncomeFromPensionsViewModel): PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(
          incomeFromPensions = answers
        ))

    val sessionSpOnly         = sessionWith(statePensionOnly)
    val sessionSpAndSpLumpSum = sessionWith(spAndSpLumpSum)
    val sessionNoClaim        = sessionWith(anIncomeFromPensionEmptyViewModel)

    def clearedJourneyFromSession(session: PensionsUserData): PensionsUserData = {
      val clearedJourneyModel = session.pensions.incomeFromPensions
        .copy(
          statePension = None,
          statePensionLumpSum = None
        )

      session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))
    }

    val service = new StatePensionService(
      mockSessionRepository,
      mockSessionService,
      mockStateBenefitsConnector,
      mockSubmissionsConnector
    )

  }

}
