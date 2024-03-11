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

import builders.StateBenefitsUserDataBuilder.{aCreateStatePensionBenefitsUD, aCreateStatePensionLumpSumBenefitsUD}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StateBenefitsConnectorISpec extends IntegrationTest {

  lazy val connector: StateBenefitsConnector         = app.injector.instanceOf[StateBenefitsConnector]
  lazy val externalConnector: StateBenefitsConnector = appWithFakeExternalCall.injector.instanceOf[StateBenefitsConnector]

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val url: String = s"/income-tax-state-benefits/claim-data/nino/$nino"

  "StateBenefitsConnector .saveClaimData" should {

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutWithHeadersCheck(url, NO_CONTENT, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Right(())
        resultSPLS shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutWithHeadersCheck(
          url,
          NO_CONTENT,
          Json.toJson(aCreateStatePensionBenefitsUD).toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid)
        stubPutWithHeadersCheck(
          url,
          NO_CONTENT,
          Json.toJson(aCreateStatePensionLumpSumBenefitsUD).toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Right(())
        resultSPLS shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutWithHeadersCheck(url, NOT_FOUND, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP: DownstreamErrorOr[Unit] =
          Await.result(externalConnector.saveClaimData(nino, aCreateStatePensionBenefitsUD)(hc, ec), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] =
          Await.result(externalConnector.saveClaimData(nino, aCreateStatePensionBenefitsUD)(hc, ec), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel.parsingError))
        resultSPLS shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutWithHeadersCheck(url, OK, Json.toJson("""{"invalid": true}""").toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        resultSPLS shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(
          url,
          INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(
          url,
          SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(url, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val resultSP: DownstreamErrorOr[Unit]   = Await.result(connector.saveClaimData(nino, aCreateStatePensionBenefitsUD), Duration.Inf)
        val resultSPLS: DownstreamErrorOr[Unit] = Await.result(connector.saveClaimData(nino, aCreateStatePensionLumpSumBenefitsUD), Duration.Inf)

        resultSP shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
        resultSPLS shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

}
