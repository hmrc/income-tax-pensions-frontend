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
import builders.PensionSchemeBuilder.aPensionScheme1
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, none}
import common.TaxYear
import mocks.{MockPensionConnector, MockSessionRepository, MockSessionService, MockSubmissionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, PensionsUserData, ServiceError}
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import models.pension.income.{
  CreateUpdatePensionIncomeRequestModel,
  ForeignPensionContainer,
  OverseasPensionContribution,
  OverseasPensionContributionContainer
}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.BAD_REQUEST
import utils.EitherTUtils.CasterOps
import utils.UnitTest

class IncomeFromOverseasPensionsServiceSpec
    extends UnitTest
    with MockPensionConnector
    with MockSessionRepository
    with MockSubmissionsConnector
    with MockSessionService {

  "saving journey answers" when {
    "downstream calls are successful" when {
      "received payment(s) from foreign pension(s)" should {
        "save the payment details alongside any prior overseas pension contributions (OPC)" in new Test {
          MockSessionService
            .loadPriorAndSession(aUser, TaxYear(taxYear))
            .returns((priorOPC, sessionWithPayments).asRight.toEitherT)

          MockPensionConnector
            .savePensionIncome(nino, taxYear, modelWithForeignPensionsAndOpc)
            .returns(().asRight.asFuture)

          MockSessionRepository
            .createOrUpdate(clearedJourneyFromSession(sessionWithPayments))
            .returns(().asRight.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe ().asRight
        }
      }
      "not receiving any payment from a foreign pension" when {
        "prior OPC claims exist" should {
          "send only the OPC claims, omitting foreign pensions" in new Test { // so to "delete" any previously claimed payment (if it exists)
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYear))
              .returns((priorOPC, sessionNoPayments).asRight.toEitherT)

            MockPensionConnector
              .savePensionIncome(nino, taxYear, modelWithOpcOnly)
              .returns(().asRight.asFuture)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionWithPayments))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight

          }
        }
        "no prior OPC exists" should {
          "call the delete pension income endpoint" in new Test {
            MockSessionService
              .loadPriorAndSession(aUser, TaxYear(taxYear))
              .returns((priorNoOPC, sessionNoPayments).asRight.toEitherT)

            MockPensionConnector
              .deletePensionIncome(nino, taxYear)
              .returns(().asRight.asFuture)

            MockSessionRepository
              .createOrUpdate(clearedJourneyFromSession(sessionWithPayments))
              .returns(().asRight.asFuture)

            val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

            result shouldBe ().asRight
          }
        }

      }
    }
    "there are unsuccessful downstream calls" when {
      "no user session is found in the database" should {
        "return SessionNotFound" in new Test {
          MockSessionService
            .loadPriorAndSession(aUser, TaxYear(taxYear))
            .returns(dataNotFoundResponse)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe DataNotFound.asLeft
        }
      }
      "the pensions downstream returns an unsuccessful result" should {
        "return an APIErrorModel" in new Test {
          MockSessionService
            .loadPriorAndSession(aUser, TaxYear(taxYear))
            .returns((priorOPC, sessionWithPayments).asRight.toEitherT)

          MockPensionConnector
            .savePensionIncome(nino, taxYear, modelWithForeignPensionsAndOpc)
            .returns(apiError.asLeft.asFuture)

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
          MockSessionService
            .loadPriorAndSession(aUser, TaxYear(taxYear))
            .returns((priorOPC, sessionWithPayments).asRight.toEitherT)

          MockPensionConnector
            .savePensionIncome(nino, taxYear, modelWithForeignPensionsAndOpc)
            .returns(().asRight.asFuture)

          MockSessionRepository
            .createOrUpdate(clearedJourneyFromSession(sessionWithPayments))
            .returns(DataNotUpdated.asLeft.asFuture)

          val result = service.saveAnswers(aUser, TaxYear(taxYear)).futureValue

          result shouldBe DataNotUpdated.asLeft
        }
      }
    }
  }

  trait Test {
    type PriorAndSession = (IncomeTaxUserData, PensionsUserData)

    def priorWith(opc: Seq[OverseasPensionContribution]): IncomeTaxUserData = {
      val income = anAllPensionsData.pensionIncome.map(_.copy(overseasPensionContribution = opc.some))

      IncomeTaxUserData(anAllPensionsData.copy(pensionIncome = income).some)
    }

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

    val priorOPC   = priorWith(opc)
    val priorNoOPC = priorWith(Seq(OverseasPensionContribution.blankSubmission))

    private def sessionWith(journeyAnswers: IncomeFromOverseasPensionsViewModel): PensionsUserData =
      aPensionsUserData.copy(
        pensions = emptyPensionsData.copy(
          incomeFromOverseasPensions = journeyAnswers
        ))

    val sessionWithPayments =
      sessionWith(
        IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = true.some,
          overseasIncomePensionSchemes = Seq(aPensionScheme1)
        ))

    val sessionNoPayments =
      sessionWith(
        IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = false.some,
          overseasIncomePensionSchemes = Seq.empty
        ))

    def downstreamIncomeModelWith(prior: IncomeTaxUserData,
                                  foreignPension: Option[ForeignPensionContainer],
                                  opc: Option[OverseasPensionContributionContainer]): CreateUpdatePensionIncomeRequestModel =
      CreateUpdatePensionIncomeRequestModel
        .fromPriorData(prior)
        .copy(foreignPension = foreignPension, overseasPensionContribution = opc)

    val modelWithForeignPensionsAndOpc = {
      val fps = sessionWithPayments.pensions.incomeFromOverseasPensions.maybeToForeignPension.value

      downstreamIncomeModelWith(priorOPC, ForeignPensionContainer(fps).some, OverseasPensionContributionContainer(opc).some)
    }

    val modelWithOpcOnly = downstreamIncomeModelWith(priorOPC, none[ForeignPensionContainer], OverseasPensionContributionContainer(opc).some)

    def clearedJourneyFromSession(session: PensionsUserData): PensionsUserData = {
      val clearedJourneyModel =
        session.pensions.copy(
          incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel.empty
        )
      session.copy(pensions = clearedJourneyModel)
    }

    val apiError: APIErrorModel = APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))

    val dataNotFoundResponse = DataNotFound.asLeft[PriorAndSession].toEitherT.leftAs[ServiceError]
    val apiErrorResponse     = apiError.asLeft[PriorAndSession].toEitherT.leftAs[ServiceError]

    val service = new IncomeFromOverseasPensionsService(mockSessionRepository, mockPensionsConnector, mockSessionService)
  }

}
