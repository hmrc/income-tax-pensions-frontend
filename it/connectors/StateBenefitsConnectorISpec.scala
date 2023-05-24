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

import builders.StateBenefitsUserDataBuilder.{aStatePensionBenefitsUD, aStatePensionLumpSumBenefitsUD}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StateBenefitsConnectorISpec extends IntegrationTest {

  lazy val connector: StateBenefitsConnector = app.injector.instanceOf[StateBenefitsConnector]
  lazy val externalConnector: StateBenefitsConnector = appWithFakeExternalCall.injector.instanceOf[StateBenefitsConnector]

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val url: String = s"/income-tax-state-benefits/claim-data/nino/$nino"

  val sPModel = aStatePensionBenefitsUD
  val sPLSModel = aStatePensionLumpSumBenefitsUD

  "StateBenefitsConnector .saveClaimData" should {

    "Return a success result" when {
      "submission returns a 204" in {
        stubPutWithHeadersCheck(
          url,
          NO_CONTENT,
          "{}",
          "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val resultSP = Await.result(connector.saveClaimData(nino, sPModel), Duration.Inf)
        val resultSPLS = Await.result(connector.saveClaimData(nino, sPLSModel), Duration.Inf)
        resultSP shouldBe Right(())
        resultSPLS shouldBe Right(())
      }
    }

    "Return an error result" when {

      "submission returns a 200 but invalid json" in {

        stubPutWithHeadersCheck(url, OK, Json.toJson("""{"invalid": true}""").toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP = Await.result(connector.saveClaimData(nino, sPModel), Duration.Inf)
        val resultSPLS = Await.result(connector.saveClaimData(nino, sPLSModel), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        resultSPLS shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(url, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP = Await.result(connector.saveClaimData(nino, sPModel), Duration.Inf)
        val resultSPLS = Await.result(connector.saveClaimData(nino, sPLSModel), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(url, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP = Await.result(connector.saveClaimData(nino, sPModel), Duration.Inf)
        val resultSPLS = Await.result(connector.saveClaimData(nino, sPLSModel), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(url, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP = Await.result(connector.saveClaimData(nino, sPModel), Duration.Inf)
        val resultSPLS = Await.result(connector.saveClaimData(nino, sPLSModel), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

}
