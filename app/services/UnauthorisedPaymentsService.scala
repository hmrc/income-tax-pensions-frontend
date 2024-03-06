/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.PensionsConnector
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{DatabaseError, PensionsUserData, ServiceError}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, UnauthorisedPaymentsViewModel}
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsService @Inject() (repository: PensionsUserDataRepository,
                                             pensionsConnector: PensionsConnector,
                                             service: PensionSessionService) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] =
    (for {
      data <- service.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      journeyAnswers   = session.pensions.unauthorisedPayments
      _ <- sendDownstream(journeyAnswers, prior, user, taxYear)(ec, hc.withMtditId(user.mtditid)).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  private def clearJourneyFromSession(session: PensionsUserData): EitherT[Future, DatabaseError, Unit] = {
    val clearedJourneyModel = session.pensions.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel())
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  private def sendDownstream(answers: UnauthorisedPaymentsViewModel, priorData: IncomeTaxUserData, user: User, taxYear: TaxYear)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): EitherT[Future, APIErrorModel, Unit] = {
    val model = buildDownstreamUpsertRequestModel(answers, priorData)
    EitherT(pensionsConnector.savePensionCharges(user.nino, taxYear.endYear, model))
  }

  private def buildDownstreamUpsertRequestModel(answers: UnauthorisedPaymentsViewModel,
                                                prior: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel =
    CreateUpdatePensionChargesRequestModel
      .fromPriorData(prior)
      .copy(pensionSchemeUnauthorisedPayments = answers.toDownstreamRequestModel.some)
}
