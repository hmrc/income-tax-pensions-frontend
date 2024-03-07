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
import connectors.httpParsers.ExcludeJourneyHttpParser._
import models.logging.ConnectorRequestInfo
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import services.DownstreamOutcome
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ExcludeJourneyConnector @Inject() (
    http: HttpClient,
    appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def excludeJourney(journeyKey: String, taxYear: Int, nino: String)(hc: HeaderCarrier): DownstreamOutcome[Int] = {
    implicit val headerCarrier: HeaderCarrier = hcWithCorrelationId(hc)

    val url = s"${appConfig.incomeTaxSubmissionBEBaseUrl}/income-tax/nino/$nino/sources/exclude-journey/$taxYear"
    ConnectorRequestInfo("POST", url, "income-tax-submission").logRequest(logger)

    http.POST[JsObject, DownstreamErrorOr[Int]](url, Json.obj("journey" -> journeyKey))
  }

}
