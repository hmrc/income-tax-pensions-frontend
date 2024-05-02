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

import common.TaxYear
import connectors.PensionsConnector
import models.User
import models.domain.ApiResultT
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait PensionsService {
  def upsertPaymentsIntoPensions(user: User, taxYear: TaxYear, paymentsIntoPensions: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit]
}

@Singleton
class PensionsServiceImpl @Inject() (pensionsConnector: PensionsConnector) extends PensionsService {
  def upsertPaymentsIntoPensions(user: User, taxYear: TaxYear, paymentsIntoPensions: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] =
    pensionsConnector.savePaymentsIntoPensions(user.getNino, taxYear, paymentsIntoPensions)(hc.withMtditId(user.mtditid), ec)

}
