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

import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionIncomeBuilder.aCreateUpdatePensionIncomeModel
import builders.PensionReliefsBuilder.anPensionReliefs
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeModel
import models.pension.reliefs.CreateOrUpdatePensionReliefsModel
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PensionsConnectorISpec extends IntegrationTest {

  lazy val connector: PensionsConnector = app.injector.instanceOf[PensionsConnector]
  lazy val externalConnector: PensionsConnector = appWithFakeExternalCall.injector.instanceOf[PensionsConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  "PensionsConnector .savePensionChargesSessionData" should {

    val chargesURL = s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear"
    val chargesRequestModel = CreateUpdatePensionChargesRequestModel(
      anPensionCharges.pensionSavingsTaxCharges,
      anPensionCharges.pensionSchemeOverseasTransfers,
      anPensionCharges.pensionSchemeUnauthorisedPayments,
      anPensionCharges.pensionContributions,
      anPensionCharges.overseasPensionContributions,
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutWithHeadersCheck(chargesURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutWithHeadersCheck(chargesURL, NO_CONTENT, Json.toJson(chargesRequestModel).toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutWithHeadersCheck(chargesURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns invalid json" in {

        stubPutWithHeadersCheck(chargesURL, INTERNAL_SERVER_ERROR, """{"invalid": true}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(chargesURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(chargesURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(chargesURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "PensionsConnector .savePensionIncomeSessionData" should {

    val incomeURL = s"/income-tax-pensions/pension-income/session-data/nino/$nino/taxYear/$taxYear"
    val incomeRequestModel = CreateUpdatePensionIncomeModel(
      aCreateUpdatePensionIncomeModel.foreignPension,
      aCreateUpdatePensionIncomeModel.overseasPensionContribution
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutWithHeadersCheck(incomeURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutWithHeadersCheck(incomeURL, NO_CONTENT, Json.toJson(incomeRequestModel).toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutWithHeadersCheck(incomeURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutWithHeadersCheck(incomeURL, OK, Json.toJson("""{"invalid": true}""").toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(incomeURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(incomeURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(incomeURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionIncomeSessionData(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "PensionsConnector .savePensionReliefSessionData" should {

    val reliefURL = s"/income-tax-pensions/pension-reliefs/nino/$nino/taxYear/$taxYear"
    val reliefRequestModel = CreateOrUpdatePensionReliefsModel(
      anPensionReliefs.pensionReliefs
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutWithHeadersCheck(reliefURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutWithHeadersCheck(reliefURL, NO_CONTENT, Json.toJson(reliefRequestModel).toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutWithHeadersCheck(reliefURL, NO_CONTENT, "{}", "X-Session-ID" -> sessionId,
          "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutWithHeadersCheck(reliefURL, OK, Json.toJson("""{"invalid": true}""").toString(),
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(reliefURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(reliefURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(reliefURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result = Await.result(connector.savePensionReliefSessionData(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }
}
