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

import cats.implicits.catsSyntaxEitherId
import connectors.IncomeTaxUserDataConnector
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import models.pension.income.{CreateUpdatePensionIncomeModel, ForeignPensionContainer, OverseasPensionContributionContainer}
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, Reliefs}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionOverseasPaymentService @Inject() (pensionUserDataRepository: PensionsUserDataRepository,
                                               pensionIncomeConnectorHelper: PensionIncomeConnectorHelper,
                                               pensionReliefsConnectorHelper: PensionReliefsConnectorHelper,
                                               incomeTaxUserDataConnector: IncomeTaxUserDataConnector) {

  private def getPensionsUserData(userData: Option[PensionsUserData], user: User, taxYear: Int)(implicit clock: Clock): PensionsUserData =
    userData match {
      case Some(value) => value.copy(pensions = value.pensions.copy(paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel()))
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

  def savePaymentsFromOverseasPensionsViewModel(user: User, taxYear: Int)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      clock: Clock): Future[Either[ServiceError, Unit]] = {

    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)

    (for {
      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
      priorData   <- FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hcWithExtras))

      viewModelPayments = sessionData.map(_.pensions.paymentsIntoPension)
      updatedReliefsData = CreateOrUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = viewModelPayments.flatMap(_.totalRASPaymentsAndTaxRelief),
          oneOffPensionContributionsPaid = viewModelPayments.flatMap(_.totalOneOffRasPaymentPlusTaxRelief),
          retirementAnnuityPayments = viewModelPayments.flatMap(_.totalRetirementAnnuityContractPayments),
          paymentToEmployersSchemeNoTaxRelief = viewModelPayments.flatMap(_.totalWorkplacePensionPayments),
          overseasPensionSchemeContributions = sessionData.flatMap(_.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount)
        )
      )

      viewModelOverseas = sessionData.map(_.pensions.paymentsIntoOverseasPensions)
      incomeSubModel = viewModelOverseas
        .map(_.toPensionContributions)
        .flatMap(seq => if (seq.nonEmpty) Some(seq) else None)
        .map(OverseasPensionContributionContainer)
      updatedIncomeData = CreateUpdatePensionIncomeModel(
        foreignPension = priorData.pensions.flatMap(_.pensionIncome.flatMap(_.foreignPension)).map(ForeignPensionContainer),
        overseasPensionContribution = incomeSubModel
      )

      _ <- FutureEitherOps[ServiceError, Unit](
        pensionReliefsConnectorHelper.sendDownstream(user.nino, taxYear, None, viewModelPayments, updatedReliefsData)(hcWithExtras, ec))

      _ <-
        if (updatedIncomeData.overseasPensionContribution.isEmpty) {
          FutureEitherOps[ServiceError, Unit](Future.successful(().asRight))
        } else {
          FutureEitherOps[ServiceError, Unit](
            pensionIncomeConnectorHelper
              .sendDownstream(user.nino, taxYear, incomeSubModel, viewModelOverseas, updatedIncomeData)(hcWithExtras, ec))
        }

      updatedCYA = getPensionsUserData(sessionData, user, taxYear)
      result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield result).value
  }
}
