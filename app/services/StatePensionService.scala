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

import connectors.StateBenefitsConnector
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError, StateBenefitsUserData}
import models.pension.statebenefits.{ClaimCYAModel, IncomeFromPensionsViewModel}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class StatePensionService @Inject()(pensionUserDataRepository: PensionsUserDataRepository, stateBenefitsConnector: StateBenefitsConnector) {

  def persistIncomeFromPensionsViewModel(user: User, taxYear: Int)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {


    //scalastyle:off method.length
    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)

    def getPensionsUserData(userData: Option[PensionsUserData], user: User): PensionsUserData = {
      userData match {
        case Some(value) => value.copy(pensions = value.pensions.copy(incomeFromPensions = IncomeFromPensionsViewModel()))
        case None => PensionsUserData(
          user.sessionId,
          user.mtditid,
          user.nino,
          taxYear,
          isPriorSubmission = false,
          PensionsCYAModel.emptyModels,
          clock.now(DateTimeZone.UTC)
        )
      }
    }

    def buildStateBenefitsUserData(sessionData: PensionsUserData, benefitType: String): StateBenefitsUserData = {
      val conditionalData = if (benefitType.equals("statePension"))
        sessionData.pensions.incomeFromPensions.statePension
      else sessionData.pensions.incomeFromPensions.statePensionLumpSum

      val claimModel = ClaimCYAModel(
        benefitId = conditionalData.flatMap(_.benefitId),
        startDate = conditionalData.flatMap(_.startDate).getOrElse(LocalDate.now()),
        endDateQuestion = conditionalData.flatMap(_.endDateQuestion),
        endDate = conditionalData.flatMap(_.endDate),
        dateIgnored = conditionalData.flatMap(_.dateIgnored),
        submittedOn = conditionalData.flatMap(_.submittedOn),
        amount = conditionalData.flatMap(_.amount),
        taxPaidQuestion = conditionalData.flatMap(_.taxPaidQuestion),
        taxPaid = conditionalData.flatMap(_.taxPaid))

      StateBenefitsUserData(
        benefitType = benefitType,
        sessionDataId = None,
        sessionId = sessionData.sessionId,
        mtdItId = sessionData.mtdItId,
        nino = sessionData.nino,
        taxYear = taxYear,
        benefitDataType = "hmrcData", //todo check "hmrcData" / "customerAdded" / "customerOverride" benefit data types
        claim = Some(claimModel),
        lastUpdated = Instant.parse(sessionData.lastUpdated.toLocalDateTime.toString)
      )
    }

    (for {
      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))

      statePensionSubmission = buildStateBenefitsUserData(sessionData.get, "statePension")
      statePensionLumpSumSubmission = buildStateBenefitsUserData(sessionData.get, "statePensionLumpSum")

      _ <- FutureEitherOps[ServiceError, Unit](stateBenefitsConnector.saveClaimData(user.nino, statePensionSubmission)(hcWithExtras, ec))
      _ <- FutureEitherOps[ServiceError, Unit](stateBenefitsConnector.saveClaimData(user.nino, statePensionLumpSumSubmission)(hcWithExtras, ec))

      updatedCYA = getPensionsUserData(sessionData, user)
      result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield {
      result
    }).value

  }
}
