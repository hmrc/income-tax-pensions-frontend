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

import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.pension.statebenefits.{AllStateBenefitsData, StateBenefit, StateBenefitsData}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class IncomeTaxUserDataConnectorSpec extends IntegrationTest with TableDrivenPropertyChecks {

  lazy val connector: IncomeTaxUserDataConnector         = app.injector.instanceOf[IncomeTaxUserDataConnector]
  lazy val externalConnector: IncomeTaxUserDataConnector = appWithFakeExternalCall.injector.instanceOf[IncomeTaxUserDataConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

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

    "getUserData (override state pension from income-tax-submission)" when {
      val now                 = LocalDate.now()
      val stateBenefitDataOld = AllStateBenefitsData(Some(StateBenefitsData(Some(Set(StateBenefit(UUID.randomUUID(), now.minusDays(10)))))))
      val stateBenefitDataNew = AllStateBenefitsData(Some(StateBenefitsData(Some(Set(StateBenefit(UUID.randomUUID(), now))))))

      val cases = Table(
        ("pension model", "state benefit model (expected)"),
        (Some(stateBenefitDataOld), Some(stateBenefitDataNew)),
        (None, Some(stateBenefitDataNew)),
        (Some(stateBenefitDataOld), None),
        (None, None)
      )

      "update our state benefit model" in forAll(cases) { case (stataBenefitInPensionModel, stateBenefitModel) =>
        stubGetWithHeadersCheck(
          s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
          OK,
          Json
            .toJson(
              userData.copy(
                pensions = userData.pensions.map(_.copy(stateBenefits = stataBenefitInPensionModel)),
                stateBenefits = stateBenefitModel
              ))
            .toString(),
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid
        )

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(nino, taxYear), Duration.Inf)
        result shouldBe Right(
          userData.copy(
            pensions = userData.pensions.map(_.copy(stateBenefits = stateBenefitModel)),
            stateBenefits = stateBenefitModel
          ))
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

}
