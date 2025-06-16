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

import cats.data.EitherT
import config.AppConfig
import connectors.Connector.hcWithCorrelationId
import connectors.httpParsers.ExcludeJourneyHttpParser.ExcludeJourneyResponseReads
import models.logging.ConnectorRequestInfo
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URI
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ExcludeJourneyConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def excludeJourney(journeyKey: String, taxYear: Int, nino: String)(hc: HeaderCarrier): DownstreamOutcome[Int] = {
    implicit val headerCarrier: HeaderCarrier = hcWithCorrelationId(hc)
    val url: URI = new URI(s"${appConfig.incomeTaxSubmissionBEBaseUrl}/income-tax/nino/$nino/sources/exclude-journey/$taxYear")

    ConnectorRequestInfo("POST", url.toString, "income-tax-submission").logRequest(logger)

    EitherT {
      httpClientV2
        .post(url.toURL)
        .withBody(Json.obj("journey" -> journeyKey))
        .execute[DownstreamErrorOr[Int]]
    }.value
  }

}
