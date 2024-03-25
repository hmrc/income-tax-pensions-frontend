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

package connectors

import config.AppConfig
import connectors.Connector.hcWithCorrelationId
import connectors.httpParsers.DeleteStateBenefitsHttpParser.DeleteStateBenefitsHttpReads
import connectors.httpParsers.SaveStateBenefitsHttpParser.SaveStateBenefitsHttpReads
import models.logging.ConnectorRequestInfo
import models.mongo.StateBenefitsUserData
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StateBenefitsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends Logging {

  val baseUrl: String = appConfig.statePensionBEBaseUrl

  def saveClaimData(nino: String, model: StateBenefitsUserData)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = baseUrl + s"/income-tax-state-benefits/claim-data/nino/$nino"
    ConnectorRequestInfo("PUT", url, "income-tax-state-benefits").logRequestWithBody(logger, model)
    http.PUT[StateBenefitsUserData, DownstreamErrorOr[Unit]](url, model)(
      StateBenefitsUserData.stateBenefitsUserDataWrites,
      SaveStateBenefitsHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deleteClaim(nino: String, taxYear: Int, sessionId: UUID)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = baseUrl + s"/income-tax-state-benefits/claim-data/nino/$nino/$taxYear/session/$sessionId/remove"
    ConnectorRequestInfo("DELETE", url, "income-tax-state-benefits").logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(
      DeleteStateBenefitsHttpReads,
      hcWithCorrelationId(hc),
      ec
    )

  }

}
