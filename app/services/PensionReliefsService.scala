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
import connectors.PensionsConnector
import models.User
import models.error.ApiError
import models.error.ApiError.CreateOrUpdateError
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.PensionsUserData
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionReliefsService @Inject() (pensionUserDataRepository: PensionsUserDataRepository,
                                       pensionReliefsConnectorHelper: PensionReliefsConnectorHelper,
                                       pensionsConnector: PensionsConnector) {

  def persistPaymentIntoPensionViewModel(user: User,
                                         taxYear: TaxYear,
                                         paymentsIntoPensions: PaymentsIntoPensionsViewModel,
                                         existingOverseasPensionSchemeContributions: Option[BigDecimal])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): EitherT[Future, ApiError, Unit] =
    (for {
      _ <- pensionsConnector.savePaymentsIntoPensions(user.getNino, taxYear, paymentsIntoPensions)(hc.withMtditId(user.mtditid), ec).leftMap { err =>
        CreateOrUpdateError(err.toString)
      }
    } yield ()).leftMap { err =>
      CreateOrUpdateError(err.toString)
    }

  private def removeSubmittedData(taxYear: TaxYear, userData: Option[PensionsUserData], user: User): PensionsUserData =
    userData
      .map(data => data.copy(pensions = data.pensions.removePaymentsIntoPension()))
      .getOrElse(PensionsUserData.empty(user, taxYear))

}
