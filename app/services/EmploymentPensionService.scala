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
import cats.implicits.toTraverseOps
import common.TaxYear
import connectors.{DownstreamErrorOr, EmploymentConnector}
import models.User
import models.logging.HeaderCarrierExtensions._
import models.mongo._
import models.pension.statebenefits.UkPensionIncomeViewModel
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResult
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentPensionService @Inject() (repository: PensionsUserDataRepository, connector: EmploymentConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] = {
    def saveJourneyData(allIncomes: Seq[UkPensionIncomeViewModel]): DownstreamOutcome[Seq[Unit]] =
      allIncomes
        .traverse[Future, DownstreamErrorOr[Unit]] { income =>
          connector.saveEmploymentPensionsData(user.nino, taxYear.endYear, income.toDownstreamRequest)(hc.withMtditId(user.mtditid), ec)
        }
        .map(sequence)

    (for {
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
      _            <- EitherT(saveJourneyData(session.pensions.incomeFromPensions.uKPensionIncomes)).leftAs[ServiceError]
      _            <- EitherT(clearJourneyFromSession(session)).leftAs[ServiceError]
    } yield ()).value

  }

  private def clearJourneyFromSession(session: PensionsUserData): QueryResult[Unit] = {
    val clearedJourneyModel =
      session.pensions.incomeFromPensions.copy(
        uKPensionIncomes = Nil,
        uKPensionIncomesQuestion = None
      )
    val updatedSessionModel =
      session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

    repository.createOrUpdate(updatedSessionModel)

  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, Seq[B]]) { (e, acc) =>
      for {
        xs <- acc
        x  <- e
      } yield xs :+ x
    }
}
