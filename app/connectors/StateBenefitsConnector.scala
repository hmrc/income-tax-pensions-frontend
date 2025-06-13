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
import connectors.httpParsers.DeleteStateBenefitsHttpParser.DeleteStateBenefitsHttpReads
import connectors.httpParsers.SaveStateBenefitsHttpParser.SaveStateBenefitsHttpReads
import models.logging.ConnectorRequestInfo
import models.mongo.StateBenefitsUserData
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import java.net.URI
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StateBenefitsConnector @Inject() (val httpClientV2: HttpClientV2, val appConfig: AppConfig) extends Logging {

  def saveClaim(nino: String,
                model: StateBenefitsUserData
               )(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url: URI = new URI(appConfig.statePensionBEBaseUrl + s"/income-tax-state-benefits/claim-data/nino/$nino")

    ConnectorRequestInfo("PUT", url.toString, "income-tax-state-benefits").logRequestWithBody(logger, model)

    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = SaveStateBenefitsHttpReads
    EitherT {
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(model))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }.value
  }

  def deleteClaim(nino: String,
                  taxYear: Int,
                  benefitId: UUID
                 )(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url: URI = new URI(appConfig.statePensionBEBaseUrl + s"/income-tax-state-benefits/claim-data/nino/$nino/$taxYear/$benefitId/remove")
    ConnectorRequestInfo("DELETE", url.toString, "income-tax-state-benefits").logRequest(logger)

    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = DeleteStateBenefitsHttpReads

    EitherT {
      httpClientV2
        .delete(url.toURL)
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }.value

  }

}
