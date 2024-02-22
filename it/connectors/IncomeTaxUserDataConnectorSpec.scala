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

import com.github.tomakehurst.wiremock.http.HttpHeader
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class IncomeTaxUserDataConnectorSpec extends IntegrationTest {

  lazy val connector: IncomeTaxUserDataConnector         = app.injector.instanceOf[IncomeTaxUserDataConnector]
  lazy val externalConnector: IncomeTaxUserDataConnector = appWithFakeExternalCall.injector.instanceOf[IncomeTaxUserDataConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val desUrl: String = s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear"

  "IncomeTaxUserDataConnector" should {
    "Return a success result" when {
      "submission returns a 204" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          NO_CONTENT,
          "{}",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Right(IncomeTaxUserData())
      }

      "submission returns a 200" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          OK,
          Json.toJson(userData).toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Right(userData)
      }

    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          OK,
          Json.toJson(userData).toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(externalConnector.getUserData(nino, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          OK,
          Json.toJson("""{"invalid": true}""").toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "refreshStateBenefits" should {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val headersSentToDes = Seq(
      new HttpHeader("mtditid", mtditid)
    )

    "succeed when correct parameters are passed" in {
      val jsValue = Json.toJson(RefreshIncomeSourceRequest("pensions"))

      stubPutWithoutResponseBody(desUrl, jsValue.toString(), NO_CONTENT, headersSentToDes)

      await(connector.refreshPensionsResponse(nino, mtditid, taxYear)(hc)) shouldBe Right(())
    }

    "return a Left error" when {

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, UNPROCESSABLE_ENTITY).foreach { errorStatus =>
        val desResponseBody = Json
          .obj(
            "code"   -> "SOME_DES_ERROR_CODE",
            "reason" -> "SOME_DES_ERROR_REASON"
          )
          .toString

        val jsValue = Json.toJson(RefreshIncomeSourceRequest("pensions"))

        s"API returns $errorStatus" in {
          val expectedResult =
            APIErrorModel(
              status = if (errorStatus == UNPROCESSABLE_ENTITY) INTERNAL_SERVER_ERROR else errorStatus,
              APIErrorBodyModel("SOME_DES_ERROR_CODE", "SOME_DES_ERROR_REASON"))

          stubPutWithResponseBody(desUrl, jsValue.toString(), desResponseBody, errorStatus)

          val result = await(connector.refreshPensionsResponse(nino, mtditid, taxYear)(hc))

          result shouldBe Left(expectedResult)
        }

        s"API returns $errorStatus response that does not have a parsable error body" in {
          val expectedResult =
            APIErrorModel(
              status = if (errorStatus == UNPROCESSABLE_ENTITY) INTERNAL_SERVER_ERROR else errorStatus,
              APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API"))

          stubPutWithResponseBody(desUrl, jsValue.toString(), "UNEXPECTED RESPONSE BODY", errorStatus)

          val result = await(connector.refreshPensionsResponse(nino, mtditid, taxYear)(hc))

          result shouldBe Left(expectedResult)
        }
      }
    }
  }

}
