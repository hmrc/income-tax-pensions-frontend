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
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, PaymentsIntoPensionsViewModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import org.joda.time.DateTimeZone
import play.api.http.Status.BAD_REQUEST
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionReliefsService @Inject() (pensionUserDataRepository: PensionsUserDataRepository,
                                       pensionReliefsConnectorHelper: PensionReliefsConnectorHelper) {

  private def removeSubmittedData(taxYear: Int, userData: Option[PensionsUserData], user: User)(implicit clock: Clock): PensionsUserData =
    userData match {
      case Some(value) => value.copy(pensions = value.pensions.copy(paymentsIntoPension = PaymentsIntoPensionsViewModel()))
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

  def persistPaymentIntoPensionViewModel(user: User, taxYear: Int, paymentsIntoPensions: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      clock: Clock): EitherT[Future, APIErrorModel, Unit] = {
    val reliefs            = paymentsIntoPensions.toReliefs
    val updatedReliefsData = CreateOrUpdatePensionReliefsModel(pensionReliefs = reliefs)

    (for {
      allSessionData <- EitherT(pensionUserDataRepository.find(taxYear, user))
      _ <- EitherT(
        pensionReliefsConnectorHelper
          .sendDownstream(user.nino, taxYear, subRequestModel = None, cya = Some(paymentsIntoPensions), requestModel = updatedReliefsData)(
            hc.withExtraHeaders("mtditid" -> user.mtditid),
            ec))
      updatedCYA = removeSubmittedData(taxYear, allSessionData, user)
      result <- EitherT[Future, ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield result).leftMap { err =>
      APIErrorModel(BAD_REQUEST, APIErrorBodyModel(BAD_REQUEST.toString, s"Unable to createOrUpdate pension service because: ${err.message}"))
    }
  }
}
