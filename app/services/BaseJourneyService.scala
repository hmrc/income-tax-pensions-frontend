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
import common.TaxYear
import connectors.IncomeTaxUserDataConnector
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import models.mongo.{PensionsUserData, ServiceError, SessionNotFound}
import models.{IncomeTaxUserData, User}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.EitherTUtils.CasterOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BaseJourneyService @Inject() (repository: PensionsUserDataRepository, submissionsConnector: IncomeTaxUserDataConnector) {

  private type PriorAndSession = (IncomeTaxUserData, PensionsUserData)

  def loadPriorAndSession(user: User, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): EitherT[Future, ServiceError, PriorAndSession] = {
    val hcWithMtdItId = hc.withMtditId(user.mtditid)
    for {
      prior        <- EitherT(submissionsConnector.getUserData(user.nino, taxYear.endYear)(hcWithMtdItId)).leftAs[ServiceError]
      maybeSession <- EitherT(repository.find(taxYear.endYear, user)).leftAs[ServiceError]
      session      <- EitherT.fromOption[Future](maybeSession, SessionNotFound).leftAs[ServiceError]
    } yield (prior, session)

  }

}
