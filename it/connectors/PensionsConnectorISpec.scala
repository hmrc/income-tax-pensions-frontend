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
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PensionsConnectorISpec extends IntegrationTest {

  lazy val connector: PensionsConnector = app.injector.instanceOf[PensionsConnector]
  lazy val externalConnector: PensionsConnector = appWithFakeExternalCall.injector.instanceOf[PensionsConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  "PensionsConnector" should {
    "Return a success result" when {
      "submission returns a 204" in {


        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", NO_CONTENT,
          "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", NO_CONTENT,
          "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )
        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", OK,
          Json.toJson("""{"invalid": true}""").toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutWithHeadersCheck(s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear", BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val model = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSavingsTaxCharges,
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions,
        )

        val result = Await.result(connector.savePensionChargesSessionData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }
}