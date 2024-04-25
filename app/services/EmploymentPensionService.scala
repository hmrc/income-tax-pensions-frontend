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
import cats.implicits._
import common.TaxYear
import connectors._
import models.User
import models.logging.HeaderCarrierExtensions._
import models.mongo.PensionsUserData.SessionData
import models.mongo._
import models.pension.employmentPensions.EmploymentPensions
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps
import utils.FutureUtils.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentPensionService @Inject() (sessionService: PensionSessionService,
                                          repository: PensionsUserDataRepository,
                                          employmentConnector: EmploymentConnector,
                                          pensionsConnector: PensionsConnector) {

  def loadPriorEmployment(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcomeT[EmploymentPensions] =
    EitherT(pensionsConnector.loadPriorEmployment(user.nino, taxYear))
      .leftAs[ServiceError]

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] =
    (for {
      maybeSession    <- EitherT(sessionService.loadSession(taxYear.endYear, user))
      session         <- EitherT.fromOption[Future](maybeSession, SessionNotFound)
      priorEmployment <- loadPriorEmployment(user, taxYear)
      _               <- processSubmission(priorEmployment, session.pensions.incomeFromPensions, user, taxYear.endYear)
      _               <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  private def processSubmission(prior: EmploymentPensions, answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): ServiceOutcomeT[Unit] = {
    val priorIds = prior.employmentData.map(_.employmentId)
    for {
      _   <- runDeleteIfRequired(priorIds, answers.uKPensionIncomes, user, taxYear)
      res <- runSaveIfRequired(answers, user, taxYear)
    } yield res
  }

  private def runDeleteIfRequired(priorIds: List[String], answers: Seq[UkPensionIncomeViewModel], user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ServiceOutcomeT[Unit] =
    (priorIds.toNel, answers.traverse(_.employmentId))
      .mapN { (pIds, sIds) =>
        val idsToDelete = pIds.toList.diff(sIds)
        doDelete(idsToDelete, user, taxYear).leftAs[ServiceError]
      }
      .getOrElse(EitherT(().asRight.toFuture))

  private def runSaveIfRequired(answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ServiceOutcomeT[Unit] = {
    val isClaimingEmployment = answers.uKPensionIncomesQuestion.exists(_ => answers.uKPensionIncomes.nonEmpty)

    if (isClaimingEmployment) doSave(answers, user, taxYear).leftAs[ServiceError]
    else EitherT(().asRight.toFuture)
  }

  private def doDelete(employmentIds: Seq[String], user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcomeT[Unit] =
    EitherT(
      employmentIds
        .traverse[Future, DownstreamErrorOr[Unit]] { id =>
          employmentConnector.deleteEmployment(user.nino, taxYear, id)(hc.withMtditId(user.mtditid), ec)
        }
        .map(sequence)).collapse

  private def doSave(answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcomeT[Unit] =
    EitherT(
      answers.uKPensionIncomes
        .traverse[Future, DownstreamErrorOr[Unit]] { e =>
          employmentConnector.saveEmployment(user.nino, taxYear, e.toDownstreamModel)(hc.withMtditId(user.mtditid), ec)
        }
        .map(sequence)).collapse

  private def clearJourneyFromSession(session: SessionData): QueryResultT[Unit] = {
    val clearedJourneyModel =
      session.pensions.incomeFromPensions.copy(
        uKPensionIncomes = Seq.empty,
        uKPensionIncomesQuestion = None
      )
    val updatedSessionModel = session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

    EitherT(repository.createOrUpdate(updatedSessionModel))

  }
}
