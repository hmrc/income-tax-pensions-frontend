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

package models.logging

import models.logging.HeaderCarrierExtensions.{CorrelationIdHeaderKey, HeaderCarrierOps}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier

final case class ConnectorRequestInfo(method: String, url: String, apiId: String)(implicit hc: HeaderCarrier) {
  private def apiIdStr = s"Sending request to API#${apiId}"

  private def connectorMessage: String =
    s"$apiIdStr $method $url with CorrelationId=[$CorrelationIdHeaderKey=${hc.maybeCorrelationId}]:"

  def logRequestWithBody[A: Writes](logger: Logger, body: A): Unit =
    logger.debug(s"$connectorMessage\n${Json.prettyPrint(implicitly[Writes[A]].writes(body))}")

  def logRequest(logger: Logger): Unit =
    logger.debug(connectorMessage)

}
