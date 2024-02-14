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
import models.User
import models.error.ApiError
import models.error.ApiError.CreateOrUpdateError
import models.mongo.{PensionsUserData, ServiceError}
import models.pension.reliefs.{CreateOrUpdatePensionReliefsModel, PaymentsIntoPensionsViewModel}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionReliefsService @Inject() (pensionUserDataRepository: PensionsUserDataRepository,
                                       pensionReliefsConnectorHelper: PensionReliefsConnectorHelper) {

  def persistPaymentIntoPensionViewModel(user: User, taxYear: TaxYear, paymentsIntoPensions: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      clock: Clock): EitherT[Future, ApiError, Unit] = {
    val reliefs            = paymentsIntoPensions.toReliefs
    val updatedReliefsData = CreateOrUpdatePensionReliefsModel(pensionReliefs = reliefs)

    (for {
      allSessionData <- EitherT(pensionUserDataRepository.find(taxYear.endYear, user))
      _ <- EitherT(
        pensionReliefsConnectorHelper
          .sendDownstream(
            user.nino,
            taxYear.endYear,
            subRequestModel = None,
            journeyAnswers = Some(paymentsIntoPensions),
            requestModel = updatedReliefsData)(hc.withExtraHeaders("mtditid" -> user.mtditid), ec))
      updatedCYA = removeSubmittedData(taxYear, allSessionData, user)
      result <- EitherT[Future, ServiceError, Unit](pensionUserDataRepository.createOrUpdate(updatedCYA))
    } yield result).leftMap { err =>
      CreateOrUpdateError(err.toString)
    }
  }

  private def removeSubmittedData(taxYear: TaxYear, userData: Option[PensionsUserData], user: User)(implicit clock: Clock): PensionsUserData =
    userData
      .map(data => data.copy(pensions = data.pensions.removePaymentsIntoPension()))
      .getOrElse(PensionsUserData.empty(user, taxYear))

}
