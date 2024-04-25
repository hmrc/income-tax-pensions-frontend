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
import cats.implicits.{catsSyntaxList, toTraverseOps}
import common.TaxYear
import connectors._
import models.mongo._
import models.pension.statebenefits.ClaimCYAModel
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResult
import services.BenefitType.{StatePension, StatePensionLumpSum}
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import java.time.{Instant, LocalDate}
import java.util.UUID
import javax.inject.{Inject, Singleton}
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

@Singleton
class StatePensionService @Inject() (repository: PensionsUserDataRepository,
                                     service: PensionSessionService,
                                     stateBenefitsConnector: StateBenefitsConnector,
                                     submissionsConnector: IncomeTaxUserDataConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] =
    (for {
      data <- service.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      _ <- EitherT(processSubmission(session, prior, taxYear.endYear, user)).leftAs[ServiceError]
      _ <- EitherT(clearJourneyFromSession(session)).leftAs[ServiceError]
    } yield ()).value

  private def processSubmission(session: PensionsUserData, prior: IncomeTaxUserData, taxYear: Int, user: User)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val reqModels = Seq(StatePension, StatePensionLumpSum).map(buildDownstreamRequestModel(_, session, taxYear))
    (for {
      _   <- runDeleteIfRequired(prior, session, user, taxYear)
      res <- reqModels.traverse(runSaveIfRequired(_, user, taxYear))
    } yield res).collapse.value
  }

  private def runDeleteIfRequired(prior: IncomeTaxUserData, session: PensionsUserData, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcomeT[Unit] = {
    val priorBenefitIds   = obtainPriorBenefitIds(prior)
    val sessionBenefitIds = obtainSessionBenefitIds(session)

    priorBenefitIds
      .diff(sessionBenefitIds)
      .toNel
      .fold(EitherT.pure[Future, APIErrorModel](())) { ids =>
        for {
          _ <- EitherT(doDelete(ids.toList, user.nino, taxYear))
          _ <- EitherT(refreshSubmissionsCache(user, taxYear))
        } yield ()
      }
  }

  private def doDelete(ids: List[UUID], nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] =
    ids
      .traverse[Future, DownstreamErrorOr[Unit]](id => stateBenefitsConnector.deleteClaim(nino, taxYear, id))
      .map(sequence)
      .map(_.collapse)

  private def runSaveIfRequired(answers: StateBenefitsUserData, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcomeT[Unit] =
    if (answers.claim.exists(_.amount.nonEmpty))
      for {
        _ <- EitherT(stateBenefitsConnector.saveClaim(user.nino, answers))
        _ <- EitherT(refreshSubmissionsCache(user, taxYear))
      } yield ()
    else EitherT.pure[Future, APIErrorModel](())

  private def obtainSessionBenefitIds(session: PensionsUserData) = {
    val maybeSpId        = session.pensions.incomeFromPensions.statePension.flatMap(_.benefitId)
    val maybeSpLumpSumId = session.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.benefitId)

    List(maybeSpId, maybeSpLumpSumId).flatten
  }

  private def obtainPriorBenefitIds(prior: IncomeTaxUserData) = {
    val maybeSbData      = prior.pensions.flatMap(_.stateBenefits.flatMap(_.stateBenefitsData))
    val maybeSpId        = maybeSbData.flatMap(_.statePension.map(_.benefitId))
    val maybeSpLumpSumId = maybeSbData.flatMap(_.statePensionLumpSum.map(_.benefitId))

    List(maybeSpId, maybeSpLumpSumId).flatten
  }

  private def clearJourneyFromSession(session: PensionsUserData): QueryResult[Unit] = {
    val clearedJourneyModel = session.pensions.incomeFromPensions
      .copy(
        statePension = None,
        statePensionLumpSum = None
      )
    val updatedSessionModel =
      session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

    repository.createOrUpdate(updatedSessionModel)
  }

  private def refreshSubmissionsCache(user: User, taxYear: Int)(implicit hc: HeaderCarrier): DownstreamOutcome[Unit] =
    submissionsConnector.refreshPensionsResponse(user.nino, user.mtditid, taxYear)

  private def buildDownstreamRequestModel(benefitType: BenefitType, session: PensionsUserData, taxYear: Int): StateBenefitsUserData = {
    val stateBenefit = benefitType match {
      case StatePension        => session.pensions.incomeFromPensions.statePension
      case StatePensionLumpSum => session.pensions.incomeFromPensions.statePensionLumpSum
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
      sessionId = session.sessionId,
      mtdItId = session.mtdItId,
      nino = session.nino,
      taxYear = taxYear,
      benefitDataType = if (claimModel.benefitId.isEmpty) "customerAdded" else "customerOverride",
      claim = Some(claimModel),
      lastUpdated = Instant.parse(session.lastUpdated.toLocalDateTime.toString + "Z")
    )
  }
}
