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

import connectors.{IncomeTaxUserDataConnector, PensionsConnector}
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, TransfersIntoOverseasPensionsViewModel, UnauthorisedPaymentsViewModel}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PensionChargesService @Inject()(pensionUserDataRepository: PensionsUserDataRepository,
                                      pensionsConnector: PensionsConnector,
                                      incomeTaxUserDataConnector: IncomeTaxUserDataConnector) {

  private def savePensionChargesData(user: User, taxYear: Int,
                             submissionModel: CreateUpdatePensionChargesRequestModel,
                             updatedCYA: PensionsUserData)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {

    pensionsConnector.savePensionChargesSessionData(user.nino, taxYear, submissionModel)(
      hc.withExtraHeaders("mtditid" -> user.mtditid), ec)

    pensionUserDataRepository.createOrUpdate(updatedCYA)
  }


  def saveUnauthorisedViewModel(user: User, taxYear: Int)(implicit hc: HeaderCarrier,
                                ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {

    (for {
      priorData <- FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector
        .getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))

      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))

      viewModel = sessionData.map(_.pensions.unauthorisedPayments)

      result <-
        FutureEitherOps[ServiceError, Unit](savePensionChargesData(
          user = user,
          taxYear = taxYear,
          submissionModel = createUnauthorisedChargesModel(viewModel, priorData),
          updatedCYA = getUnauthorisedPaymentsUserData(sessionData, user, taxYear, clock)
        ))
    } yield {
      result
    }).value
  }

  private def getUnauthorisedPaymentsUserData(userData: Option[PensionsUserData],
                                              user: User, taxYear: Int, clock: Clock): PensionsUserData = {
    userData match {
      case Some(value) => value.copy(pensions = value.pensions.copy(
        unauthorisedPayments = UnauthorisedPaymentsViewModel()
      ))
      case None => PensionsUserData(
        user.sessionId, user.mtditid,
        user.nino, taxYear,
        isPriorSubmission = false,
        PensionsCYAModel.emptyModels,
        clock.now(DateTimeZone.UTC)
      )
    }
  }
  private def createUnauthorisedChargesModel(viewModel: Option[UnauthorisedPaymentsViewModel],
                                             priorData: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel = {
    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
      pensionSchemeOverseasTransfers = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)),
      pensionSchemeUnauthorisedPayments = viewModel.map(_.toUnauth),
      pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
      overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
    )
  }




  def saveTransfersIntoOverseasPensionsViewModel(user: User, taxYear: Int)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {

    (for {
      priorData <- FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector
        .getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))

      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))

      viewModel = sessionData.map(_.pensions.transfersIntoOverseasPensions)

      result <-
        FutureEitherOps[ServiceError, Unit](savePensionChargesData(
          user = user,
          taxYear = taxYear,
          submissionModel = createTransfersIOSChargesModel(viewModel, priorData),
          updatedCYA = getTransfersIntoOverseasUserData(sessionData, user, taxYear, clock)
        ))
    } yield {
      result
    }).value
  }

  private def getTransfersIntoOverseasUserData(userData: Option[PensionsUserData],
                                               user: User, taxYear: Int, clock: Clock): PensionsUserData = {
    userData match {
      case Some(value) => value.copy(pensions = value.pensions.copy(
        transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel()
      ))
      case None => PensionsUserData(
        user.sessionId, user.mtditid,
        user.nino, taxYear,
        isPriorSubmission = false,
        PensionsCYAModel.emptyModels,
        clock.now(DateTimeZone.UTC)
      )
    }
  }
  private def createTransfersIOSChargesModel(viewModel: Option[TransfersIntoOverseasPensionsViewModel],
                                             priorData: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel = {
    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
      pensionSchemeOverseasTransfers = viewModel.map(_.toTransfersIOP),
      pensionSchemeUnauthorisedPayments = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)),
      pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
      overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
    )
  }


}




