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
import cats.implicits.{catsSyntaxEitherId, catsSyntaxTuple2Semigroupal, toTraverseOps}
import common.TaxYear
import connectors.{DownstreamErrorOr, DownstreamOutcome, EmploymentConnector, IncomeTaxUserDataConnector}
import models.IncomeTaxUserData.PriorData
import models.User
import models.logging.HeaderCarrierExtensions._
import models.mongo.PensionsUserData.SessionData
import models.mongo._
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
                                          submissionsConnector: IncomeTaxUserDataConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] =
    (for {
      data <- sessionService.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      answers          = session.pensions.incomeFromPensions
      _ <- processSubmission(prior, answers, user, taxYear.endYear)
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  private def processSubmission(prior: PriorData, answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): ServiceOutcomeT[Seq[Unit]] = {
    val priorEmployment = prior.pensions.flatMap(_.employmentPensions)
    val maybePriorIds   = priorEmployment.map(_.employmentData.map(_.employmentId))

    for {
      _   <- runDeleteIfRequired(maybePriorIds, answers.uKPensionIncomes, user, taxYear)
      res <- runSaveIfRequired(answers, user, taxYear)
    } yield res

  }

  private def runDeleteIfRequired(maybePriorIds: Option[Seq[String]], answers: Seq[UkPensionIncomeViewModel], user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ServiceOutcomeT[Seq[Unit]] =
    (maybePriorIds, answers.traverse(_.employmentId))
      .mapN { (priorIds, sessionIds) =>
        val idsToDelete = priorIds.diff(sessionIds)
        for {
          res <- EitherT(doDelete(idsToDelete, user, taxYear)).leftAs[ServiceError]
          _   <- EitherT(refreshSubmissionsCache(user, taxYear)).leftAs[ServiceError]
        } yield res
      }
      .getOrElse(EitherT(Seq.empty[Unit].asRight.toFuture))

  private def runSaveIfRequired(answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ServiceOutcomeT[Seq[Unit]] = {
    val isClaimingEmployment = answers.uKPensionIncomesQuestion.exists(_ => answers.uKPensionIncomes.nonEmpty)

    if (isClaimingEmployment) {
      for {
        res <- EitherT(doSave(answers, user, taxYear)).leftAs[ServiceError]
        _   <- EitherT(refreshSubmissionsCache(user, taxYear)).leftAs[ServiceError]
      } yield res

    } else EitherT(Seq.empty[Unit].asRight.toFuture)
  }

  private def refreshSubmissionsCache(user: User, taxYear: Int)(implicit hc: HeaderCarrier): DownstreamOutcome[Unit] =
    submissionsConnector.refreshPensionsResponse(user.nino, user.mtditid, taxYear)

  private def doDelete(employmentIds: Seq[String], user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Seq[Unit]] =
    employmentIds
      .traverse[Future, DownstreamErrorOr[Unit]] { id =>
        employmentConnector.deleteEmployment(user.nino, taxYear, id)(hc.withMtditId(user.mtditid), ec)
      }
      .map(sequence)

  private def doSave(answers: IncomeFromPensionsViewModel, user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Seq[Unit]] =
    answers.uKPensionIncomes
      .traverse[Future, DownstreamErrorOr[Unit]] { e =>
        employmentConnector.saveEmployment(user.nino, taxYear, e.toDownstreamModel)(hc.withMtditId(user.mtditid), ec)
      }
      .map(sequence)

  private def clearJourneyFromSession(session: SessionData): QueryResultT[Unit] = {
    val clearedJourneyModel =
      session.pensions.incomeFromPensions.copy(
        uKPensionIncomes = Seq.empty,
        uKPensionIncomesQuestion = None
      )
    val updatedSessionModel = session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

    EitherT(repository.createOrUpdate(updatedSessionModel))

  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, Seq[B]]) { (e, acc) =>
      for {
        xs <- acc
        x  <- e
      } yield xs :+ x
    }

}
