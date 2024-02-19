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
import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.http.HttpResponse

class ConnectorResponseInfoSpec extends AnyWordSpecLike {
  private val connectorInfo = ConnectorResponseInfo("GET", "/someurl", HttpResponse(200, ""))

  "logMessageWarnOn4xx" should {
    "return info on success" in {
      assert(
        connectorInfo.logResponseWarnOn4xx === Info("Connector [X-CorrelationId=unknown]: Response Received for GET /someurl. Response status: 200"))
    }

    "return info on success with correlationId" in {
      assert(
        connectorInfo.copy(response = HttpResponse(200, "", Map(CorrelationIdHeaderKey -> List("just-for-test")))).logResponseWarnOn4xx === Info(
          "Connector [X-CorrelationId=just-for-test]: Response Received for GET /someurl. Response status: 200"))
    }

    "return warn on 4xx error with body" in {
      val badRequest = connectorInfo.copy(response = HttpResponse(400, "some error"))
      assert(
        badRequest.logResponseWarnOn4xx === Warn(
          "Connector [X-CorrelationId=unknown]: Response Received for GET /someurl. Response status: 400 Body: some error"))
    }

    "return error on 5xx error with body" in {
      val errorRequest = connectorInfo.copy(response = HttpResponse(500, "some error"))
      assert(
        errorRequest.logResponseWarnOn4xx === Error(
          "Connector [X-CorrelationId=unknown]: Response Received for GET /someurl. Response status: 500 Body: some error"))
    }
  }

}
