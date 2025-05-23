/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import config.AppConfig
import models.IncomeTaxUserData
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.Future

class IncomeTaxUserDataConnector @Inject() (val http: HttpClientV2, val config: AppConfig) extends Logging {

  def getUserData(nino: String, taxYear: Int)(hc: HeaderCarrier): DownstreamOutcome[IncomeTaxUserData] =
    Future.successful(
      Right(IncomeTaxUserData(None, None))
    ) // TODO This is an old way of loading prior data (before we had the database) - it will be removed

  def refreshPensionsResponse(nino: String, mtditid: String, taxYear: Int)(implicit hc: HeaderCarrier): DownstreamOutcome[Unit] =
    // TODO This will be removed, we don't use cache anymore
    Future.successful(Right(()))

}
