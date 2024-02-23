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
import builders.PaymentsIntoOverseasPensionsViewModelBuilder._
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import common.TaxYear
import mocks.{MockPensionConnector, MockSessionRepository, MockSubmissionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import models.pension.income.{CreateUpdatePensionIncomeRequestModel, OverseasPensionContribution, OverseasPensionContributionContainer}
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.BAD_REQUEST
import utils.Constants.zero
import utils.UnitTest

class PaymentsIntoOverseasPensionsServiceSpec extends UnitTest with MockPensionConnector with MockSessionRepository with MockSubmissionsConnector {

  "saving journey answers" when {
    "all external calls are successful" when {
      "claiming an amount paid into overseas pensions" when {
        "not claiming for overseas pension contributions (OPC)" when {
          "no prior OPC claim exists" should {
            "successfully save answers to relief only" in new Test with SuccessfulMocks {
              priorReturns(priorNoOPC)
              sessionHas(sessionNoOPCWithPayment)
              reliefsExpects(populatedReliefsModel)
              sessionRepositoryExpects(sessionNoOPCWithPayment)

              val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

              result shouldBe ().asRight
            }
          }
          "a prior OPC claim exists" should {
            "save answers to relief and remove the prior OPC claim by sending a blank submission" in new Test with SuccessfulMocks {
              priorReturns(priorOPC)
              sessionHas(sessionNoOPCWithPayment)
              reliefsExpects(populatedReliefsModel)
              incomeExpects(emptyOPCIncomeModel)
              sessionRepositoryExpects(sessionNoOPCWithPayment)

              val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

              result shouldBe ().asRight
            }
          }
        }
        "claiming for OPC" should {
          "successfully save answers to both relief and income" in new Test with SuccessfulMocks {
            priorReturns(priorOPC)
            sessionHas(sessionOPC)
            reliefsExpects(populatedReliefsModel)
            incomeExpects(populatedOPCIncomeModel)
            sessionRepositoryExpects(sessionOPC)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }

      }
      "no amount is claimed into an overseas pension" when {
        "a prior OPC claim exists" should {
          "send blank reliefs submission and delete the prior income claim" in new Test with SuccessfulMocks {
            priorReturns(priorOPC)
            sessionHas(sessionNoOPCNoPayment)
            reliefsExpects(blankReliefsModel(priorOPC))
            incomeExpects(emptyOPCIncomeModel)
            sessionRepositoryExpects(sessionNoOPCNoPayment)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }

        }
        "no prior OPC claim exists" should {
          "send a blank reliefs submission only" in new Test with SuccessfulMocks {
            priorReturns(priorNoOPC)
            sessionHas(sessionNoOPCNoPayment)
            reliefsExpects(blankReliefsModel(priorNoOPC))
            sessionRepositoryExpects(sessionNoOPCNoPayment)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }
      }
    }
    "no user session is found in the database" should {
      "return SessionNotFound" in new Test with SuccessfulMocks {
        priorReturns(IncomeTaxUserData(None))

        MockSessionRepository
          .find(taxYear, aUser)
          .returns(DataNotFound.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe DataNotFound.asLeft
      }
    }

    "pensions reliefs downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test with SuccessfulMocks {
        priorReturns(priorOPC)
        sessionHas(sessionOPC)

        MockPensionConnector
          .savePensionReliefs(nino, taxYear, populatedReliefsModel)
          .returns(apiError.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe apiError.asLeft
      }
    }
    "pensions income downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test with SuccessfulMocks {
        priorReturns(priorOPC)
        sessionHas(sessionOPC)
        reliefsExpects(populatedReliefsModel)

        MockPensionConnector
          .savePensionIncome(nino, taxYear, populatedOPCIncomeModel)
          .returns(apiError.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe apiError.asLeft
      }
    }

    "submissions downstream returns an unsuccessful result" should {
      "return an APIErrorModel" in new Test with SuccessfulMocks {
        MockSubmissionsConnector
          .getUserData(nino, taxYear)
          .returns(apiError.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe apiError.asLeft
      }
    }

    "session data could not be updated" should {
      "return DataNotUpdated" in new Test with SuccessfulMocks {
        priorReturns(priorOPC)
        sessionHas(sessionOPC)
        reliefsExpects(populatedReliefsModel)
        incomeExpects(populatedOPCIncomeModel)

        MockSessionRepository
          .createOrUpdate(clearedJourneyFromSession(sessionOPC))
          .returns(DataNotUpdated.asLeft.asFuture)

        val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

        result shouldBe DataNotUpdated.asLeft
      }
    }
  }

  trait Test {

    val opc = Seq(
      OverseasPensionContribution(
        customerReference = Some("PENSIONINCOME245"),
        exemptEmployersPensionContribs = 1999.99,
        migrantMemReliefQopsRefNo = None,
        dblTaxationRelief = None,
        dblTaxationCountry = None,
        dblTaxationArticle = None,
        dblTaxationTreaty = None,
        sf74Reference = Some("SF74-123456")
      ))

    def priorWith(opc: Seq[OverseasPensionContribution]): IncomeTaxUserData = {
      val income = anAllPensionsData.pensionIncome.map(_.copy(overseasPensionContribution = opc.some))

      IncomeTaxUserData(anAllPensionsData.copy(pensionIncome = income).some)
    }

    val priorOPC   = priorWith(opc)
    val priorNoOPC = priorWith(Seq(OverseasPensionContribution.blankSubmission))

    val answersOPC              = aPaymentsIntoOverseasPensionsViewModel
    val answersNoOPCWithPayment = aPaymentsIntoOverseasPensionsNoReliefsViewModel
    val answersNoPayment        = paymentsIntoOverseasPensionsNoPaymentViewModel

    private def sessionWith(journeyAnswers: PaymentsIntoOverseasPensionsViewModel): PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(
          paymentsIntoOverseasPensions = journeyAnswers
        ))

    val sessionOPC              = sessionWith(answersOPC)
    val sessionNoOPCWithPayment = sessionWith(answersNoOPCWithPayment)
    val sessionNoOPCNoPayment   = sessionWith(answersNoPayment)

    val populatedReliefsModel = reliefsModelWith(priorOPC, answersOPC.paymentsIntoOverseasPensionsAmount)

    def blankReliefsModel(prior: IncomeTaxUserData): CreateUpdatePensionReliefsModel = reliefsModelWith(prior, zero.some)

    def reliefsModelWith(prior: IncomeTaxUserData, amount: Option[BigDecimal]): CreateUpdatePensionReliefsModel = {
      val reliefs = CreateUpdatePensionReliefsModel
        .fromPriorData(prior)
        .pensionReliefs
        .copy(overseasPensionSchemeContributions = amount)

      CreateUpdatePensionReliefsModel(reliefs)
    }

    private def incomeModelWithOPC(opc: Seq[OverseasPensionContribution]): CreateUpdatePensionIncomeRequestModel =
      CreateUpdatePensionIncomeRequestModel
        .fromPriorData(priorOPC)
        .copy(overseasPensionContribution = OverseasPensionContributionContainer(opc).some)

    val populatedOPCIncomeModel = incomeModelWithOPC(answersOPC.toDownstreamOverseasPensionContribution)
    val emptyOPCIncomeModel     = incomeModelWithOPC(Seq(OverseasPensionContribution.blankSubmission))

    val apiError: APIErrorModel = APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))

    val service = new PaymentsIntoOverseasPensionsService(mockSessionRepository, mockSubmissionsConnector, mockPensionsConnector)
  }

  trait SuccessfulMocks {

    def priorReturns(prior: IncomeTaxUserData) =
      MockSubmissionsConnector
        .getUserData(nino, taxYear)
        .returns(prior.asRight.asFuture)

    def sessionHas(session: PensionsUserData) =
      MockSessionRepository
        .find(taxYear, aUser)
        .returns(session.some.asRight.asFuture)

    def reliefsExpects(model: CreateUpdatePensionReliefsModel) =
      MockPensionConnector
        .savePensionReliefs(nino, taxYear, model)
        .returns(().asRight.asFuture)

    def incomeExpects(model: CreateUpdatePensionIncomeRequestModel) =
      MockPensionConnector
        .savePensionIncome(nino, taxYear, model)
        .returns(().asRight.asFuture)

    def sessionRepositoryExpects(session: PensionsUserData) =
      MockSessionRepository
        .createOrUpdate(clearedJourneyFromSession(session))
        .returns(().asRight.asFuture)

    def clearedJourneyFromSession(session: PensionsUserData): PensionsUserData = {
      val clearedJourneyModel =
        session.pensions.copy(
          paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel.empty
        )
      session.copy(pensions = clearedJourneyModel)
    }
  }
}
