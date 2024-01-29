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

import connectors.EmploymentConnector
import connectors.httpParsers.EmploymentSessionHttpParser.EmploymentSessionResponse
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentPensionService @Inject() (pensionUserDataRepository: PensionsUserDataRepository, employmentConnector: EmploymentConnector) {

  def persistUkPensionIncomeViewModel(user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      clock: Clock): Future[Either[ServiceError, Unit]] = {

    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)

    def getPensionsUserData(userData: Option[PensionsUserData], user: User): PensionsUserData =
      userData match {
        case Some(value) =>
          value.copy(pensions = value.pensions.copy(incomeFromPensions = value.pensions.incomeFromPensions.copy(uKPensionIncomes = Nil)))
        case None =>
          PensionsUserData(
            user.sessionId,
            user.mtditid,
            user.nino,
            taxYear,
            isPriorSubmission = false,
            PensionsCYAModel.emptyModels,
            clock.now(DateTimeZone.UTC)
          )
      }

    (for {
      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
      optUkPensionIncomes = sessionData.map(p => p.pensions.incomeFromPensions.uKPensionIncomes)
      saveEmployments = optUkPensionIncomes.fold(Seq[Future[EmploymentSessionResponse]]())(_.map(ukPensionIncome =>
        employmentConnector.saveEmploymentPensionsData(user.nino, taxYear, ukPensionIncome.toCreateUpdateEmploymentRequest)(hcWithExtras, ec)))
      _ <- FutureEitherOps[ServiceError, Seq[Unit]](Future.sequence(saveEmployments).map(sequence))
      updatedCYA = getPensionsUserData(sessionData, user)
      result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield result).value
  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, Seq[B]]) { (e, acc) =>
      for {
        xs <- acc
        x  <- e
      } yield xs :+ x
    }
}
