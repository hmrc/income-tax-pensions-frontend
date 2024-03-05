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

import cats.data.EitherT
import common.TaxYear
import connectors.{IncomeTaxUserDataConnector, StateBenefitsConnector}
import models.User
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo._
import models.pension.statebenefits.ClaimCYAModel
import repositories.PensionsUserDataRepository
import services.BenefitType.{StatePension, StatePensionLumpSum}
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait BenefitType {
  val value: String
}

object BenefitType {
  case object StatePension extends BenefitType {
    override val value: String = "statePension"
  }
  case object StatePensionLumpSum extends BenefitType {
    override val value: String = "statePensionLumpSum"
  }
}

class StatePensionService @Inject() (repository: PensionsUserDataRepository,
                                     connector: StateBenefitsConnector,
                                     incomeTaxSubmissionConnector: IncomeTaxUserDataConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {
    val hcWithMtdItId = hc.withMtditId(user.mtditid)

    (for {
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
      statePensionSubmission        = buildDownstreamRequestModel(session, StatePension, taxYear.endYear)
      statePensionLumpSumSubmission = buildDownstreamRequestModel(session, StatePensionLumpSum, taxYear.endYear)
      _ <- processClaim(statePensionSubmission, hcWithMtdItId, user)
      _ <- processClaim(statePensionLumpSumSubmission, hcWithMtdItId, user)
      _ <- EitherT(clearJourneyFromSession(session)).leftAs[ServiceError]
      _ <- EitherT(incomeTaxSubmissionConnector.refreshPensionsResponse(user.nino, user.mtditid, taxYear.endYear)).leftAs[ServiceError]
    } yield ()).value
  }

  private def clearJourneyFromSession(session: PensionsUserData): Future[Either[DatabaseError, Unit]] = {
    val clearedJourneyModel =
      session.pensions.incomeFromPensions.copy(
        statePension = None,
        statePensionLumpSum = None
      )
    val updatedSessionModel =
      session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

    repository.createOrUpdate(updatedSessionModel)
  }

  private def processClaim(answers: StateBenefitsUserData, hc: HeaderCarrier, user: User)(implicit
      ec: ExecutionContext): EitherT[Future, ServiceError, Unit] =
    if (answers.claim.exists(_.amount.nonEmpty)) {
      EitherT(connector.saveClaimData(user.nino, answers)(hc, ec)).leftAs[ServiceError]
    } else {
      EitherT.pure[Future, ServiceError](())
    }

  private def buildDownstreamRequestModel(sessionData: PensionsUserData, benefitType: BenefitType, taxYear: Int): StateBenefitsUserData = {
    val stateBenefit = benefitType match {
      case StatePension        => sessionData.pensions.incomeFromPensions.statePension
      case StatePensionLumpSum => sessionData.pensions.incomeFromPensions.statePensionLumpSum
    }

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
      benefitType = benefitType.value,
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
}
