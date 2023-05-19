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

import connectors.{IncomeTaxUserDataConnector, StateBenefitsConnector}
import models.{IncomeTaxUserData, User}
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, IncomeFromOverseasPensionsViewModel}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}
import models.pension.income.CreateUpdatePensionIncomeModel
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, Reliefs}
import models.pension.statebenefits.IncomeFromPensionsViewModel

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class StatePensionService @Inject()(pensionUserDataRepository: PensionsUserDataRepository, stateBenefitsConnector: StateBenefitsConnector,
                                     incomeTaxUserDataConnector: IncomeTaxUserDataConnector) {

  def persistIncomeFromPensionsViewModel(user: User, taxYear: Int)
                                             (implicit hc: HeaderCarrier, ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {


    //scalastyle:off method.length
    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)


    def getPensionsUserData(userData: Option[PensionsUserData], user: User): PensionsUserData = {
      userData match {
        case Some(value) => value.copy(pensions = value.pensions.copy(incomeFromPensions = IncomeFromPensionsViewModel()))
        case None => PensionsUserData(
          user.sessionId,
          user.mtditid,
          user.nino,
          taxYear,
          isPriorSubmission = false,
          PensionsCYAModel.emptyModels,
          clock.now(DateTimeZone.UTC)
        )
      }
    }

    (for {
      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
      priorData <- FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector.
        getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))

      iFPViewModel = sessionData.map(_.pensions.incomeFromOverseasPensions)
      updatedFp = iFPViewModel.map(_.toForeignPension)
      updatedPIData = CreateUpdatePensionIncomeModel(
        foreignPension = updatedFp,
        overseasPensionContribution = priorData.pensions.flatMap(_.pensionIncome.map(y => y.overseasPensionContribution))
      )

      //todo input data?
      pensionCYAModel = sessionData.map(_.pensions)
      updatedPCRData = CreateUpdatePensionChargesRequestModel(
        pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
        pensionSchemeOverseasTransfers = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)),
        pensionSchemeUnauthorisedPayments = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)),
        pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
        overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
      )

      pIPViewModel = sessionData.map(_.pensions.paymentsIntoPension)
      updatedPRData = CreateOrUpdatePensionReliefsModel(
        pensionReliefs = Reliefs(
          regularPensionContributions = pIPViewModel.flatMap(_.totalRASPaymentsAndTaxRelief),
          oneOffPensionContributionsPaid = pIPViewModel.flatMap(_.totalOneOffRasPaymentPlusTaxRelief),
          retirementAnnuityPayments = pIPViewModel.flatMap(_.totalRetirementAnnuityContractPayments),
          paymentToEmployersSchemeNoTaxRelief = pIPViewModel.flatMap(_.totalWorkplacePensionPayments),
          overseasPensionSchemeContributions = sessionData.flatMap(_.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount)
        )
      )

      _ <- FutureEitherOps[ServiceError, Unit](stateBenefitsConnector.createSessionDataID(updatedPIData)(hcWithExtras, ec))

      updatedCYA = getPensionsUserData(sessionData, user)
      result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield {
      result
    }).value

  }
}
