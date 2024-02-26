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

package services

import connectors.NrsConnector
import connectors.httpParsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class NrsServiceSpec extends UnitTest {

  val connector: NrsConnector = mock[NrsConnector]
  val service: NrsService     = new NrsService(connector)

  implicit val writesObject: Writes[String] = (o: String) => JsString(o)

  ".postNrsConnector" when {

    "there is a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right((): Unit)

        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector
          .postNrsConnector(_: String, _: String)(_: HeaderCarrier)(_: Writes[String]))
          .expects(
            nino,
            "pensions",
            headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> mtditid, "clientIP" -> "127.0.0.1", "clientPort" -> "80"),
            writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, "pensions", mtditid)(headerCarrierWithTrueClientDetails, writesObject))

        result shouldBe expectedResult
      }
    }

    "there isn't a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right((): Unit)

        (connector
          .postNrsConnector(_: String, _: String)(_: HeaderCarrier)(_: Writes[String]))
          .expects(nino, "pensions", headerCarrierWithSession.withExtraHeaders("mtditid" -> mtditid), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, "pensions", mtditid))

        result shouldBe expectedResult
      }
    }

  }

}
