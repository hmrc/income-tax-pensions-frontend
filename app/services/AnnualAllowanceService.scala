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
import cats.implicits.catsSyntaxOptionId
import common.TaxYear
import connectors.{IncomeTaxUserDataConnector, PensionsConnector}
import models.mongo.{DatabaseError, PensionsUserData, ServiceError, SessionNotFound}
import models.pension.charges._
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.EitherTOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnnualAllowanceService @Inject() (repository: PensionsUserDataRepository,
                                        pensionsConnector: PensionsConnector,
                                        submissionsConnector: IncomeTaxUserDataConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {
    val hcWithMtdItId = hc.addMtdItId(user)
    (for {
      priorData    <- EitherT(submissionsConnector.getUserData(user.nino, taxYear.endYear)(hcWithMtdItId)).leftAs[ServiceError]
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
      journeyAnswers = session.pensions.pensionsAnnualAllowances
      _ <- sendDownstream(journeyAnswers, priorData, user, taxYear)(ec, hcWithMtdItId).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value
  }

  private def clearJourneyFromSession(session: PensionsUserData): EitherT[Future, DatabaseError, Unit] = {
    val clearedJourneyModel =
      session.pensions.copy(
        pensionsAnnualAllowances = PensionAnnualAllowancesViewModel()
      )
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  private def sendDownstream(answers: PensionAnnualAllowancesViewModel, priorData: IncomeTaxUserData, user: User, taxYear: TaxYear)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): EitherT[Future, APIErrorModel, Unit] =
    answers.reducedAnnualAllowanceQuestion
      .fold(EitherT.pure[Future, APIErrorModel](())) { _ =>
        val model = buildDownstreamUpsertRequestModel(answers, priorData)
        EitherT(pensionsConnector.savePensionCharges(user.nino, taxYear.endYear, model))
      }

  private def buildDownstreamUpsertRequestModel(answers: PensionAnnualAllowancesViewModel,
                                                prior: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel =
    CreateUpdatePensionChargesRequestModel
      .fromPriorData(prior)
      .copy(pensionContributions = answers.toPensionContributions.some)
}