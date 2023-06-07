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
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, PensionSchemeUnauthorisedPayments, ShortServiceRefundsViewModel, TransfersIntoOverseasPensionsViewModel, UnauthorisedPaymentsViewModel}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, FutureEitherOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PensionChargesService @Inject()(pensionUserDataRepository: PensionsUserDataRepository,
                                      pensionsConnector: PensionsConnector,
                                      pensionChargesHelper: PensionChargesConnectorHelper,
                                      incomeTaxUserDataConnector: IncomeTaxUserDataConnector) {


  def saveUnauthorisedViewModel(user: User, taxYear: Int)(implicit hc: HeaderCarrier,
                                ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {

    val hcWithExtras = hc.withExtraHeaders("mtditid" -> user.mtditid)
    
    def getPensionsUserData(userData: Option[PensionsUserData], user: User): PensionsUserData = {
      userData match {
        case Some(value) => value.copy(pensions = value.pensions.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel()))
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

    def createUnauthorisedChargesModel(viewModel: Option[UnauthorisedPaymentsViewModel],
                                               priorData: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel = {
      CreateUpdatePensionChargesRequestModel(
        pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
        pensionSchemeOverseasTransfers = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)),
        pensionSchemeUnauthorisedPayments = viewModel.map(_.toUnauth),
        pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
        overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
      )
    }
    
    (
      for {
        sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
        priorData <-
          FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector
            .getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))

        currentData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
        viewModel: Option[UnauthorisedPaymentsViewModel] = sessionData.map(_.pensions.unauthorisedPayments)
        unauthModel: Option[PensionSchemeUnauthorisedPayments] = viewModel.map(_.toUnauth)

        requestModel = createUnauthorisedChargesModel(viewModel, priorData)
        
        _ <- FutureEitherOps[ServiceError, Unit](pensionChargesHelper.sendDownstream(
          user.nino, taxYear, unauthModel, viewModel, requestModel)(hcWithExtras, ec))
        updatedCYA = getPensionsUserData(currentData, user)
        result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
      } yield {
        result
      }).value
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
          submissionModel = createTransfersIOPChargesModel(viewModel, priorData),
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
  private def createTransfersIOPChargesModel(viewModel: Option[TransfersIntoOverseasPensionsViewModel],
                                             priorData: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel = {
    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
      pensionSchemeOverseasTransfers = viewModel.map(_.toTransfersIOP),
      pensionSchemeUnauthorisedPayments = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)),
      pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
      overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
    )
  }


  private def savePensionChargesData(user: User, taxYear: Int,
                                     submissionModel: CreateUpdatePensionChargesRequestModel,
                                     updatedCYA: PensionsUserData)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ServiceError, Unit]] = {

    pensionsConnector.savePensionChargesSessionData(user.nino, taxYear, submissionModel)(
      hc.withExtraHeaders("mtditid" -> user.mtditid), ec)

    pensionUserDataRepository.createOrUpdate(updatedCYA)
  }


  def saveShortServiceRefundsViewModel(user: User, taxYear: Int)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {
    (for {
      priorData <- FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector
        .getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))
      sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
      viewModel = sessionData.map(_.pensions.shortServiceRefunds)
      result <-
        FutureEitherOps[ServiceError, Unit](savePensionChargesData(
          user = user,
          taxYear = taxYear,
          submissionModel = createShortServiceRefundsChargesModel(viewModel, priorData),
          updatedCYA = getShortServiceRefundsUserData(sessionData, user, taxYear, clock)
        ))
    } yield {
      result
    }).value
  }

  private def createShortServiceRefundsChargesModel(viewModel: Option[ShortServiceRefundsViewModel],
                                             priorData: IncomeTaxUserData): CreateUpdatePensionChargesRequestModel = {
    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
      pensionSchemeOverseasTransfers = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)),
      pensionSchemeUnauthorisedPayments = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)),
      pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
      overseasPensionContributions = viewModel.map(_.toOverseasPensionContributions)
    )
  }

  private def getShortServiceRefundsUserData(userData: Option[PensionsUserData],
                                               user: User, taxYear: Int, clock: Clock): PensionsUserData = {
    userData match {
      case Some(value) => value.copy(pensions = value.pensions.copy(
        shortServiceRefunds = ShortServiceRefundsViewModel()
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
}




