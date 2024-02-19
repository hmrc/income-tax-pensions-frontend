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

import models.logging.ConnectorResponseInfo.LevelLogging._
import models.logging.CorrelationId.HttpResponseOps
import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import play.api.Logger
import uk.gov.hmrc.http.HttpResponse

final case class ConnectorResponseInfo(method: String, url: String, response: HttpResponse) {
  private def logMessage: String = {
    val nonSuccessBody = if (response.status < 200 || response.status >= 300) s" Body: ${response.body}" else ""
    s"Connector [$CorrelationIdHeaderKey=${response.correlationId}]: Response Received for $method $url. Response status: ${response.status}" + nonSuccessBody
  }

  private[logging] def logResponseWarnOn4xx: ConnectorResponseInfo.LevelLogging = {
    val msg = PIIMaskingConverter.mask(logMessage)
    if (response.status <= 399) {
      Info(msg)
    } else if (response.status >= 400 && response.status < 500) {
      Warn(msg)
    } else {
      Error(msg)
    }
  }

  def logResponseWarnOn4xx(logger: Logger): Unit =
    logResponseWarnOn4xx.log(logger)
}

object ConnectorResponseInfo {
  sealed abstract class LevelLogging {
    def log(logger: Logger): Unit = this match {
      case Info(msg)  => logger.info(msg)
      case Warn(msg)  => logger.warn(msg)
      case Error(msg) => logger.error(msg)
    }
  }

  object LevelLogging {
    final case class Info(value: String)  extends LevelLogging
    final case class Warn(value: String)  extends LevelLogging
    final case class Error(value: String) extends LevelLogging
  }
}
