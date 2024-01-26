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

import play.api.Logger
import play.api.libs.json.Writes

final case class ConnectorRequestInfo(method: String, url: String, apiId: String) {
  private def apiIdStr = s"API#${apiId}"

  def logRequestWithBody[A: Writes](logger: Logger, body: A): Unit =
    logger.debug(s"Connector: $apiIdStr $method $url\nRequest Body: ${implicitly[Writes[A]].writes(body)}")


  def logRequest(logger: Logger): Unit =
    logger.debug(s"Connector: Sending Request $method $url")

}
