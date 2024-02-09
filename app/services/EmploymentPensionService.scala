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

import cats.Foldable
import cats.data.EitherT
import cats.implicits.{toFoldableOps, toTraverseOps}
import connectors.EmploymentConnector
import models.mongo._
import models.pension.statebenefits.UkPensionIncomeViewModel
import models.{APIErrorModel, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.EitherTOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentPensionService @Inject() (repository: PensionsUserDataRepository, connector: EmploymentConnector) {

  def persistJourneyAnswers(user: User, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {
    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)

    def clearJourneyFromSession(session: PensionsUserData): Future[Either[DatabaseError, Unit]] = {
      val clearedJourneyModel =
        session.pensions.incomeFromPensions.copy(
          uKPensionIncomes = Nil,
          uKPensionIncomesQuestion = None
        )
      val updatedSessionModel =
        session.copy(pensions = session.pensions.copy(incomeFromPensions = clearedJourneyModel))

      repository.createOrUpdate(updatedSessionModel)
    }
    def saveJourneyData(allIncomes: Seq[UkPensionIncomeViewModel]): Future[Either[APIErrorModel, Seq[Unit]]] =
      allIncomes
        .traverse(income => connector.saveEmploymentPensionsData(user.nino, taxYear, income.toDownstreamRequest)(hcWithExtras, ec))
        .map(sequence)

    (for {
      maybeSession <- EitherT(repository.find(taxYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
      _            <- EitherT(saveJourneyData(session.pensions.incomeFromPensions.uKPensionIncomes)).leftAs[ServiceError]
      _            <- EitherT(clearJourneyFromSession(session)).leftAs[ServiceError]
    } yield ()).value
  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, Seq[B]]) { (e, acc) =>
      for {
        xs <- acc
        x  <- e
      } yield xs :+ x
    }
}
