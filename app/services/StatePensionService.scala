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
import models.pension.statebenefits.{ClaimCYAModel, IncomeFromPensionsViewModel, StateBenefitViewModel}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StatePensionService @Inject() (pensionUserDataRepository: PensionsUserDataRepository, stateBenefitsConnector: StateBenefitsConnector) {

  def persistStatePensionIncomeViewModel(user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      clock: Clock): Future[Either[ServiceError, Unit]] = {

    // scalastyle:off method.length
    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)

    def getPensionsUserData(userData: Option[PensionsUserData], user: User): PensionsUserData = {
      userData.getOrElse(
        PensionsUserData(
          user.sessionId,
          user.mtditid,
          user.nino,
          taxYear,
          isPriorSubmission = false,
          PensionsCYAModel.emptyModels,
          clock.now(DateTimeZone.UTC)
        )
      )
    }

    def buildStateBenefitsUserData(sessionData: PensionsUserData,
                                   stateBenefit: Option[StateBenefitViewModel],
                                   benefitType: String): StateBenefitsUserData = {

      val claimModel = ClaimCYAModel(
        benefitId = stateBenefit.flatMap(_.benefitId),
        startDate = stateBenefit.flatMap(_.startDate).getOrElse(LocalDate.now()),
        endDateQuestion = stateBenefit.flatMap(_.endDateQuestion),
        endDate = stateBenefit.flatMap(_.endDate),
        dateIgnored = stateBenefit.flatMap(_.dateIgnored),
        submittedOn = stateBenefit.flatMap(_.submittedOn),
        amount = stateBenefit.flatMap(_.amount),
        taxPaidQuestion = stateBenefit.flatMap(_.taxPaidQuestion),
        taxPaid = stateBenefit.flatMap(_.taxPaid)
      )

      StateBenefitsUserData(
        benefitType = benefitType,
        sessionDataId = None,
        sessionId = sessionData.sessionId,
        mtdItId = sessionData.mtdItId,
        nino = sessionData.nino,
        taxYear = taxYear,
        benefitDataType = if (claimModel.benefitId.isEmpty) "customerAdded" else "customerOverride",
        claim = Some(claimModel),
        lastUpdated = Instant.parse(sessionData.lastUpdated.toLocalDateTime.toString + "Z")
      )
    }

    (for {
      optSessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))

      sessionData = optSessionData.getOrElse(throw new RuntimeException("Session data is empty"))

      statePensionSubmission = buildStateBenefitsUserData(
        sessionData,
        sessionData.pensions.incomeFromPensions.statePension,
        "statePension"
      )
      statePensionLumpSumSubmission = buildStateBenefitsUserData(
        sessionData,
        sessionData.pensions.incomeFromPensions.statePensionLumpSum,
        "statePensionLumpSum"
      )

      _ <- FutureEitherOps[ServiceError, Unit] {
        if (statePensionSubmission.claim.get.amount.nonEmpty) {
          stateBenefitsConnector.saveClaimData(user.nino, statePensionSubmission)(hcWithExtras, ec)
        } else {
          Future(Right(()))
        }
      }
      _ <- FutureEitherOps[ServiceError, Unit] {
        if (statePensionLumpSumSubmission.claim.get.amount.nonEmpty) {
          stateBenefitsConnector.saveClaimData(user.nino, statePensionLumpSumSubmission)(hcWithExtras, ec)
        } else {
          Future(Right(()))
        }
      }

      updatedCYA = getPensionsUserData(Some(sessionData), user)
      result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield result).value

  }
}
