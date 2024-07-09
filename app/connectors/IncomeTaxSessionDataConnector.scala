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

package connectors

import models.logging.ConnectorRequestInfo
import models.sessionData.SessionData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait IncomeTaxSessionDataConnector {
  def getSessionData(sessionDataId: String)(implicit hc: HeaderCarrier): Future[DownstreamErrorOr[Option[SessionData]]]
}

@Singleton
class IncomeTaxSessionDataConnectorImpl @Inject() (http: HttpClient)(implicit ec: ExecutionContext)
    extends IncomeTaxSessionDataConnector
    with Logging {
  private val serviceName = "income-tax-session-data"

  def getSessionData(sessionDataId: String)(implicit hc: HeaderCarrier): Future[DownstreamErrorOr[Option[SessionData]]] = {
    val url = s"http://localhost:9000/income-tax-session-data/$sessionDataId"
    ConnectorRequestInfo("GET", url, serviceName).logRequest(logger)
    http.GET[DownstreamErrorOr[Option[SessionData]]](url)
  }
}
