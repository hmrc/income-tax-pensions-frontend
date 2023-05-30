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
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, UnauthorisedPaymentsViewModel}
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
  def saveUnauthorisedViewModel(user: User, taxYear: Int)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext, clock: Clock): Future[Either[ServiceError, Unit]] = {


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


    (
      for {
        sessionData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
        priorData <-
          FutureEitherOps[ServiceError, IncomeTaxUserData](incomeTaxUserDataConnector
            .getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)))

        currentData <- FutureEitherOps[ServiceError, Option[PensionsUserData]](pensionUserDataRepository.find(taxYear, user))
        viewModel = sessionData.map(_.pensions.unauthorisedPayments)

        req = CreateUpdatePensionChargesRequestModel(
          pensionSavingsTaxCharges = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)),
          pensionSchemeOverseasTransfers = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)),
          pensionSchemeUnauthorisedPayments = viewModel.map(_.toUnauth),
          pensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.pensionContributions)),
          overseasPensionContributions = priorData.pensions.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions))
        )
        _ <- FutureEitherOps[ServiceError, Unit](pensionsConnector.savePensionChargesSessionData(user.nino, taxYear, req)(hcWithExtras, ec))
        updatedCYA = getPensionsUserData(currentData, user)
        result <- FutureEitherOps[ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
      } yield {
        result
      }).value
  }
}




