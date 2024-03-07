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
import cats.implicits.{catsSyntaxOptionId, none}
import common.TaxYear
import connectors.PensionsConnector
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{PensionsUserData, ServiceError}
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import models.pension.income.{CreateUpdatePensionIncomeRequestModel, ForeignPensionContainer}
import models.{APIErrorModel, IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeFromOverseasPensionsService @Inject() (repository: PensionsUserDataRepository,
                                                   pensionsConnector: PensionsConnector,
                                                   service: PensionSessionService) {

  def saveAnswers(user: User, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): ServiceOutcome[Unit] =
    (for {
      data <- service.loadPriorAndSession(user, taxYear)
      (prior, session) = data
      journeyAnswers   = session.pensions.incomeFromOverseasPensions
      _ <- processClaim(journeyAnswers, prior, user, taxYear)(ec, hc.withMtditId(user.mtditid)).leftAs[ServiceError]
      _ <- clearJourneyFromSession(session).leftAs[ServiceError]
    } yield ()).value

  private def clearJourneyFromSession(session: PensionsUserData): QueryResultT[Unit] = {
    val clearedJourneyModel = session.pensions.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel.empty)
    val updatedSessionModel = session.copy(pensions = clearedJourneyModel)

    EitherT(repository.createOrUpdate(updatedSessionModel))
  }

  private def processClaim(answers: IncomeFromOverseasPensionsViewModel, prior: IncomeTaxUserData, user: User, taxYear: TaxYear)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): DownstreamOutcomeT[Unit] = {
    val hasPriorOPC =
      prior.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution.map(OPCs => OPCs.forall(_.isBlankSubmission)))).contains(false)

    answers.paymentsFromOverseasPensionsQuestion.fold(ifEmpty = EitherT.pure[Future, APIErrorModel](())) { bool =>
      if (bool || hasPriorOPC)
        EitherT(pensionsConnector.savePensionIncome(user.nino, taxYear.endYear, buildDownstreamUpsertRequestModel(answers, prior)))
      else EitherT(pensionsConnector.deletePensionIncome(user.nino, taxYear.endYear))
    }
  }

  private def buildDownstreamUpsertRequestModel(answers: IncomeFromOverseasPensionsViewModel,
                                                prior: IncomeTaxUserData): CreateUpdatePensionIncomeRequestModel =
    CreateUpdatePensionIncomeRequestModel
      .fromPriorData(prior)
      .copy(foreignPension = answers.maybeToForeignPension.fold(ifEmpty = none[ForeignPensionContainer])(ForeignPensionContainer(_).some))
}
