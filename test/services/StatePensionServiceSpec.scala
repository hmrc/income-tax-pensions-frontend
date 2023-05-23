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
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockIncomeTaxUserDataConnector, MockPensionUserDataRepository, MockStateBenefitsConnector}
import connectors.StateBenefitsConnector
import models.mongo.{DataNotFound, DataNotUpdated, StateBenefitsUserData}
import models.pension.income.CreateUpdatePensionIncomeModel
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, Reliefs}
import models.pension.statebenefits.ClaimCYAModel
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import java.time.{Instant, LocalDate}
import utils.UnitTest

import java.time.Instant

class StatePensionServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockStateBenefitsConnector
  with MockIncomeTaxUserDataConnector
  with ScalaFutures {

  val statePensionService = new StatePensionService(mockPensionUserDataRepository, mockStateBenefitsConnector)

  ".persistIncomeFromPensionsViewModel" should {
    "return Right(Unit) when StatePension and StatePensionLumpSum models are saved successfully and income from pensions cya is cleared from DB" in {
      val sessionCya = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions)
      val sessionUserData = aPensionsUserData.copy(pensions = sessionCya)

      mockFind(taxYear, aUser, Right(Option(sessionUserData)))

      val dataSP = sessionUserData.pensions.incomeFromPensions.statePension
      val dataSPLS = sessionUserData.pensions.incomeFromPensions.statePensionLumpSum
      val statePensionModel = StateBenefitsUserData(
        benefitType = "statePension",
        sessionDataId = None,
        sessionId = sessionId,
        mtdItId = mtditid,
        nino = nino,
        taxYear = taxYear,
        benefitDataType = "hmrcData",
        claim = Some(ClaimCYAModel(
          benefitId = dataSP.flatMap(_.benefitId),
          startDate = dataSP.flatMap(_.startDate).getOrElse(LocalDate.now()),
          endDateQuestion = dataSP.flatMap(_.endDateQuestion),
          endDate = dataSP.flatMap(_.endDate),
          dateIgnored = dataSP.flatMap(_.dateIgnored),
          submittedOn = dataSP.flatMap(_.submittedOn),
          amount = dataSP.flatMap(_.amount),
          taxPaidQuestion = dataSP.flatMap(_.taxPaidQuestion),
          taxPaid = dataSP.flatMap(_.taxPaid))),
        lastUpdated = Instant.parse(sessionUserData.lastUpdated.toLocalDateTime.toString)
      )
      val lumpSumModel = StateBenefitsUserData(
        benefitType = "statePensionLumpSum",
        sessionDataId = None,
        sessionId = sessionId,
        mtdItId = mtditid,
        nino = nino,
        taxYear = taxYear,
        benefitDataType = "hmrcData",
        claim = Some(ClaimCYAModel(
          benefitId = dataSPLS.flatMap(_.benefitId),
          startDate = dataSPLS.flatMap(_.startDate).getOrElse(LocalDate.now()),
          endDateQuestion = dataSPLS.flatMap(_.endDateQuestion),
          endDate = dataSPLS.flatMap(_.endDate),
          dateIgnored = dataSPLS.flatMap(_.dateIgnored),
          submittedOn = dataSPLS.flatMap(_.submittedOn),
          amount = dataSPLS.flatMap(_.amount),
          taxPaidQuestion = dataSPLS.flatMap(_.taxPaidQuestion),
          taxPaid = dataSPLS.flatMap(_.taxPaid))),
        lastUpdated = Instant.parse(sessionUserData.lastUpdated.toLocalDateTime.toString)
      )

      val userWithEmptySaveIncomeFromPensionsCya = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)
      mockSaveClaimData(nino, statePensionModel, Right(()))
      mockSaveClaimData(nino, lumpSumModel, Right(()))
      mockCreateOrUpdate(userWithEmptySaveIncomeFromPensionsCya, Right(()))

      val result = await(statePensionService.persistIncomeFromPensionsViewModel(aUser, taxYear))
      result shouldBe Right(())
    }
  }
}
