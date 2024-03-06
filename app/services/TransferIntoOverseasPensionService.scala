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
import common.TaxYear
import connectors.PensionsConnector
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{DatabaseError, PensionsUserData, ServiceError}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, TransfersIntoOverseasPensionsViewModel}
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransferIntoOverseasPensionService @Inject() (repository: PensionsUserDataRepository,
                                                    pensionsConnector: PensionsConnector,
                                                    service: PensionSessionService) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] =
    (for {
      data <- service.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      journeyAnswers   = session.pensions.transfersIntoOverseasPensions
      _ <- sendDownstream(journeyAnswers, prior, user, taxYear)(ec, hc.withMtditId(user.mtditid)).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  private def clearJourneyFromSession(session: PensionsUserData): EitherT[Future, DatabaseError, Unit] = {
    val clearedJourneyModel = session.pensions.copy(transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel.empty)
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  /** When calling `savePensionCharges`, the `CreateUpdatePensionChargesRequestModel` cannot have all fields as `None`, as API 1673/1868 does not
    * allow an empty JSON payload. As a result, we have to call the delete endpoint.
    */
  private def sendDownstream(answers: TransfersIntoOverseasPensionsViewModel, prior: IncomeTaxUserData, user: User, taxYear: TaxYear)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): EitherT[Future, APIErrorModel, Unit] = {
    val isProvidingSchemeDetails = answers.transferPensionScheme.nonEmpty

    if (hasOtherPriorChargesSubmissions(prior) || isProvidingSchemeDetails)
      EitherT(pensionsConnector.savePensionCharges(user.nino, taxYear.endYear, buildDownstreamUpsertRequestModel(answers, prior)))
    else
      EitherT(pensionsConnector.deletePensionCharges(user.nino, taxYear.endYear))
  }

  private def hasOtherPriorChargesSubmissions(prior: IncomeTaxUserData): Boolean = {
    val priorCharges = prior.pensions.flatMap(_.pensionCharges)

    val hasPriorPSTC = priorCharges.flatMap(_.pensionSavingsTaxCharges).isDefined
    val hasPriorPSUP = priorCharges.flatMap(_.pensionSchemeUnauthorisedPayments).isDefined
    val hasPriorPC   = priorCharges.flatMap(_.pensionContributions).isDefined
    val hasPriorOPC  = priorCharges.flatMap(_.overseasPensionContributions).isDefined

    if (hasPriorPSTC || hasPriorPSUP || hasPriorPC || hasPriorOPC) true
    else false
  }

  private def buildDownstreamUpsertRequestModel(answers: TransfersIntoOverseasPensionsViewModel,
                                                prior: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel =
    CreateUpdatePensionChargesRequestModel
      .fromPriorData(prior)
      .copy(pensionSchemeOverseasTransfers = answers.maybeToDownstreamRequestModel)
}
