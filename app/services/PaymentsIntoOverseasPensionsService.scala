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
import connectors.{IncomeTaxUserDataConnector, PensionsConnector}
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{DatabaseError, PensionsUserData, ServiceError, SessionNotFound}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import models.pension.income.{CreateUpdatePensionIncomeRequestModel, OverseasPensionContribution, OverseasPensionContributionContainer}
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.zero
import utils.EitherTUtils.EitherTOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsIntoOverseasPensionsService @Inject() (repository: PensionsUserDataRepository,
                                                     submissionsConnector: IncomeTaxUserDataConnector,
                                                     pensionsConnector: PensionsConnector) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {
    val hcWithMtdItId = hc.withMtditId(user.mtditid)

    def processReliefsClaim(prior: IncomeTaxUserData, journey: PaymentsIntoOverseasPensionsViewModel): EitherT[Future, APIErrorModel, Unit] = {
      val downstreamModel = journey.paymentsIntoOverseasPensionsAmount
        .fold(ifEmpty = buildDownstreamReliefsModel(prior, opscAmount = zero))(amount => buildDownstreamReliefsModel(prior, opscAmount = amount))

      EitherT(pensionsConnector.savePensionReliefs(user.nino, taxYear.endYear, downstreamModel)(hcWithMtdItId, ec))
    }

    def processIncomeClaim(prior: IncomeTaxUserData, journey: PaymentsIntoOverseasPensionsViewModel): EitherT[Future, APIErrorModel, Unit] = {
      val hasPreviousOPCSubmission =
        prior.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution.map(OPCs => OPCs.forall(_.isBlankSubmission)))).contains(false)
      val areClaimingOPC      = journey.reliefs.nonEmpty
      val intentIsToDeleteOPC = journey.reliefs.isEmpty && hasPreviousOPCSubmission

      val incomeModel =
        if (areClaimingOPC) buildDownstreamIncomeModel(prior, maybeClaim = journey.some)
        else buildDownstreamIncomeModel(prior, maybeClaim = None)

      if (areClaimingOPC || intentIsToDeleteOPC)
        EitherT(pensionsConnector.savePensionIncome(user.nino, taxYear.endYear, incomeModel)(hcWithMtdItId, ec))
      else
        EitherT.pure[Future, APIErrorModel](())
    }

    (for {
      priorData    <- EitherT(submissionsConnector.getUserData(user.nino, taxYear.endYear)(hcWithMtdItId)).leftAs[ServiceError]
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
      journeyAnswers = session.pensions.paymentsIntoOverseasPensions
      _ <- processReliefsClaim(priorData, journeyAnswers).leftAs[ServiceError]
      _ <- processIncomeClaim(priorData, journeyAnswers).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  }

  private def clearJourneyFromSession(session: PensionsUserData): EitherT[Future, DatabaseError, Unit] = {
    val clearedJourneyModel = session.pensions.copy(paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel())
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  private def buildDownstreamIncomeModel(prior: IncomeTaxUserData,
                                         maybeClaim: Option[PaymentsIntoOverseasPensionsViewModel]): CreateUpdatePensionIncomeRequestModel = {
    val modelFromPrior = CreateUpdatePensionIncomeRequestModel.fromPriorData(prior)

    maybeClaim match {
      case Some(claim) =>
        modelFromPrior.copy(overseasPensionContribution = OverseasPensionContributionContainer(claim.toDownstreamOverseasPensionContribution).some)
      case None =>
        val maybePriorOPCs = modelFromPrior.overseasPensionContribution.map(_.opc)
        // So to "logically" delete all OPCs
        val maybeUpdatedOPCs    = maybePriorOPCs.map(opcs => opcs.map(_ => OverseasPensionContribution.blankSubmission))
        val updatedOPCContainer = maybeUpdatedOPCs.flatMap(opcs => modelFromPrior.overseasPensionContribution.map(_.copy(opc = opcs)))
        modelFromPrior.copy(overseasPensionContribution = updatedOPCContainer)
    }
  }

  private def buildDownstreamReliefsModel(prior: IncomeTaxUserData, opscAmount: BigDecimal): CreateUpdatePensionReliefsModel = {
    val modelFromPrior = CreateUpdatePensionReliefsModel.fromPriorData(prior)

    modelFromPrior.copy(
      pensionReliefs = modelFromPrior.pensionReliefs.copy(overseasPensionSchemeContributions = opscAmount.some)
    )
  }
}
