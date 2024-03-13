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
import config.ErrorHandler
import connectors.{DownstreamOutcomeT, PensionsConnector}
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{PensionsUserData, ServiceError}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, ShortServiceRefundsViewModel}
import models.{IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ShortServiceRefundsService @Inject() (sessionService: PensionSessionService,
                                            repository: PensionsUserDataRepository,
                                            pensionsConnector: PensionsConnector,
                                            errorHandler: ErrorHandler) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] =
    (for {
      data <- sessionService.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      journeyAnswers   = session.pensions.shortServiceRefunds
      _ <- sendDownstream(journeyAnswers, prior, user, taxYear)(ec, hc.withMtditId(user.mtditid)).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  /** When calling `savePensionCharges`, the `CreateUpdatePensionChargesRequestModel` cannot have all fields as `None`, as API 1673/1868 does not
    * allow an empty JSON payload. As a result, we have to call the delete endpoint.
    */
  private def sendDownstream(answers: ShortServiceRefundsViewModel, prior: IncomeTaxUserData, user: User, taxYear: TaxYear)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): DownstreamOutcomeT[Unit] = {
    val isProvidingSchemeDetails = answers.refundPensionScheme.nonEmpty

    if (hasOtherPriorChargesSubmissions(prior) || isProvidingSchemeDetails)
      EitherT(pensionsConnector.savePensionCharges(user.nino, taxYear.endYear, buildDownstreamUpsertRequestModel(answers, prior)))
    else EitherT(pensionsConnector.deletePensionCharges(user.nino, taxYear.endYear))
  }

  private def hasOtherPriorChargesSubmissions(prior: IncomeTaxUserData): Boolean = {
    val priorCharges = prior.pensions.flatMap(_.pensionCharges)

    val hasPriorPSTC = priorCharges.flatMap(_.pensionSavingsTaxCharges).isDefined
    val hasPriorPSUP = priorCharges.flatMap(_.pensionSchemeUnauthorisedPayments).isDefined
    val hasPriorPC   = priorCharges.flatMap(_.pensionContributions).isDefined
    val hasPriorPSOT = priorCharges.flatMap(_.pensionSchemeOverseasTransfers).isDefined

    if (hasPriorPSTC || hasPriorPSUP || hasPriorPC || hasPriorPSOT) true
    else false
  }

  private def clearJourneyFromSession(session: PensionsUserData): QueryResultT[Unit] = {
    val clearedJourneyModel = session.pensions.copy(shortServiceRefunds = ShortServiceRefundsViewModel.empty)
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  private def buildDownstreamUpsertRequestModel(answers: ShortServiceRefundsViewModel,
                                                prior: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel =
    CreateUpdatePensionChargesRequestModel
      .fromPriorData(prior)
      .copy(overseasPensionContributions = answers.maybeToDownstreamModel)

}
