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
import common.Nino
import models.mongo.JourneyStatus.{Completed, InProgress}
import models.mongo.{JourneyContext, Mtditid}
import models.pension.Journey.{PaymentsIntoPensions, UnauthorisedPayments}
import models.pension.JourneyNameAndStatus
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, UnauthorisedPaymentsViewModel}
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.{CreateUpdatePensionReliefsModel, PaymentsIntoPensionsViewModel}
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.EitherValues._
import org.scalatest.Inside.inside
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import play.api.libs.json.Json
import testdata.{PaymentsIntoPensionsViewModelTestData, UnauthorisedPaymentsTestData}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PensionsConnectorISpec extends IntegrationTest with ScalaFutures {
  lazy val connector: PensionsConnector         = app.injector.instanceOf[PensionsConnector]
  lazy val externalConnector: PensionsConnector = appWithFakeExternalCall.injector.instanceOf[PensionsConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  def stubGetAnswers(url: String, status: Int, body: String) =
    stubGetWithHeadersCheck(url, status, body, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

  def stubPutAnswers(url: String, status: Int, body: String) =
    stubPutWithHeadersCheck(url, status, body, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

  "savePaymentsIntoPensions" should {
    val url = s"/income-tax-pensions/$currTaxYear/payments-into-pensions/$nino/answers"

    "successfully submit answers" in {
      stubPutAnswers(url, NO_CONTENT, "{}")
      val result = connector.savePaymentsIntoPensions(Nino(nino), currTaxYear, PaymentsIntoPensionsViewModelTestData.answers).value.futureValue
      result shouldBe Right(())
    }
  }

  "PensionsConnector .savePensionChargesSessionData" should {

    val chargesURL = s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear"
    val chargesRequestModel = CreateUpdatePensionChargesRequestModel(
      anPensionCharges.pensionSavingsTaxCharges,
      anPensionCharges.pensionSchemeOverseasTransfers,
      anPensionCharges.pensionSchemeUnauthorisedPayments,
      anPensionCharges.pensionContributions,
      anPensionCharges.overseasPensionContributions
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutAnswers(chargesURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutAnswers(chargesURL, NO_CONTENT, Json.toJson(chargesRequestModel).toString())

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutAnswers(chargesURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns invalid json" in {

        stubPutAnswers(chargesURL, INTERNAL_SERVER_ERROR, """{"invalid": true}""")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutAnswers(chargesURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutAnswers(chargesURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutAnswers(chargesURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionCharges(nino, taxYear, chargesRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "PensionsConnector .savePensionIncomeSessionData" should {

    val incomeURL = s"/income-tax-pensions/pension-income/session-data/nino/$nino/taxYear/$taxYear"
    val incomeRequestModel = CreateUpdatePensionIncomeRequestModel(
      aCreateUpdatePensionIncomeModel.foreignPension,
      aCreateUpdatePensionIncomeModel.overseasPensionContribution
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutAnswers(incomeURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutAnswers(incomeURL, NO_CONTENT, Json.toJson(incomeRequestModel).toString())

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutAnswers(incomeURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutAnswers(incomeURL, OK, Json.toJson("""{"invalid": true}""").toString())

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutAnswers(incomeURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutAnswers(incomeURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutAnswers(incomeURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionIncome(nino, taxYear, incomeRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "PensionsConnector .savePensionReliefSessionData" should {

    val reliefURL = s"/income-tax-pensions/pension-reliefs/nino/$nino/taxYear/$taxYear"
    val reliefRequestModel = CreateUpdatePensionReliefsModel(
      anPensionReliefs.pensionReliefs
    )

    "Return a success 204 result" when {
      "submission has no content" in {

        stubPutAnswers(reliefURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Right(())
      }

      "payload is successfully submitted" in {

        stubPutAnswers(reliefURL, NO_CONTENT, Json.toJson(reliefRequestModel).toString())

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      "the stub isn't matched due to the call being external as the headers won't be passed along" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubPutAnswers(reliefURL, NO_CONTENT, "{}")

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {

        stubPutAnswers(reliefURL, OK, Json.toJson("""{"invalid": true}""").toString())

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubPutAnswers(reliefURL, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubPutAnswers(reliefURL, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubPutAnswers(reliefURL, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")

        val result = Await.result(connector.savePensionReliefs(nino, taxYear, reliefRequestModel), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "getAllJourneyStatuses" should {
    val url               = s"/income-tax-pensions/journey-statuses/taxYear/$taxYear"
    val journeyStatusList = List(JourneyNameAndStatus(PaymentsIntoPensions, Completed), JourneyNameAndStatus(UnauthorisedPayments, InProgress))

    "Return a success 204 result" when {
      "there are no statuses saved in the backend database and an empty List is returned" in {
        stubGetAnswers(url, OK, Json.toJson(List[JourneyNameAndStatus]()).toString())
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Right(List.empty)
      }
      "there are valid statuses saved in the backend database and they are returned in the response body" in {
        stubGetAnswers(url, OK, Json.toJson(journeyStatusList).toString())
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Right(journeyStatusList)
      }
    }

    "Return an error result" when {
      "the stub isn't matched due to the call being external as the headers won't be passed along" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)
        stubGetAnswers(url, NO_CONTENT, "{}")
        val result = Await.result(connector.getAllJourneyStatuses(taxyear)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
      "submission returns a 200 but invalid json" in {
        stubGetAnswers(url, OK, Json.toJson("""{"invalid": true}""").toString())
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
      "submission returns a 500" in {
        stubGetAnswers(url, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
      "submission returns a 503" in {
        stubGetAnswers(url, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }
      "submission returns an unexpected result" in {
        stubGetAnswers(url, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getAllJourneyStatuses(taxyear), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "getJourneyStatus" should {
    val url               = s"/income-tax-pensions/journey-status/$PaymentsIntoPensions/taxYear/$taxYear"
    val journeyStatusList = List(JourneyNameAndStatus(PaymentsIntoPensions, Completed))
    val ctx               = JourneyContext(taxyear, Mtditid(mtditid), PaymentsIntoPensions)

    "Return a success 204 result" when {
      "there are no statuses saved in the backend database and an empty List is returned" in {
        stubGetAnswers(url, OK, Json.toJson(List[JourneyNameAndStatus]()).toString())
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Right(List())
      }

      "there is a valid status saved in the backend database and they are returned in the response body" in {
        stubGetAnswers(url, OK, Json.toJson(journeyStatusList).toString())
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Right(journeyStatusList)
      }
    }

    "Return an error result" when {
      "the stub isn't matched due to the call being external as the headers won't be passed along" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)
        stubGetAnswers(url, NO_CONTENT, "{}")
        val result = Await.result(connector.getJourneyStatus(ctx)(hc, ec), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {
        stubGetAnswers(url, OK, Json.toJson("""{"invalid": true}""").toString())
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {
        stubGetAnswers(url, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {
        stubGetAnswers(url, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {
        stubGetAnswers(url, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.getJourneyStatus(ctx), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

  "saveJourneyStatus" should {
    val url = s"/income-tax-pensions/journey-status/$PaymentsIntoPensions/taxYear/$taxYear"
    val ctx = JourneyContext(taxyear, Mtditid(mtditid), PaymentsIntoPensions)

    "Return a success 204 result" when {
      "a status is updated and nothing is returned" in {
        stubPutAnswers(url, NO_CONTENT, "{}")
        val result = Await.result(connector.saveJourneyStatus(ctx, Completed), Duration.Inf)

        result shouldBe Right(())
      }
    }

    "Return an error result" when {
      "the stub isn't matched due to the call being external as the headers won't be passed along" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubPutAnswers(url, INTERNAL_SERVER_ERROR, "{}")
        val result = Await.result(connector.saveJourneyStatus(ctx, Completed)(hc, ec), Duration.Inf)

        result should be(Symbol("left"))
        inside(result) { case Left(error) =>
          error.status shouldBe INTERNAL_SERVER_ERROR
          error.body shouldBe an[APIErrorBodyModel]
          error.toJson.as[APIErrorBodyModel].code shouldBe "PARSING_ERROR"
          error.toJson.as[APIErrorBodyModel].reason should include("Error parsing response from API:")
          error.toJson.as[APIErrorBodyModel].reason should include("Header does not match")
        }
      }

      "submission returns a 500" in {
        stubPutAnswers(url, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.saveJourneyStatus(ctx, Completed), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {
        stubPutAnswers(url, SERVICE_UNAVAILABLE, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.saveJourneyStatus(ctx, Completed), Duration.Inf)

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {
        stubPutAnswers(url, BAD_REQUEST, """{"code": "FAILED", "reason": "failed"}""")
        val result = Await.result(connector.saveJourneyStatus(ctx, Completed), Duration.Inf)

        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }
    }

    "getPaymentsIntoPensions" should {
      val url = s"/income-tax-pensions/$taxYear/payments-into-pensions/$nino/answers"

      "return None if no data is found" in {
        stubGetAnswers(url, OK, Json.obj().toString())
        val result = connector.getPaymentsIntoPensions(currNino, taxyear).value.futureValue
        assert(result.value === Some(PaymentsIntoPensionsViewModel.empty))
      }

      "return answers" in {
        stubGetAnswers(url, OK, Json.toJson(PaymentsIntoPensionsViewModelTestData.answers).toString())
        val result = connector.getPaymentsIntoPensions(currNino, taxyear).value.futureValue
        assert(result.value === Some(PaymentsIntoPensionsViewModelTestData.answers))
      }
    }

    "getUnauthorisedPaymentsFromPensions" should {
      val url = s"/income-tax-pensions/$taxYear/unauthorised-payments/$nino/answers"

      "return None if no data is found" in {
        stubGetAnswers(url, OK, Json.obj().toString())
        val result = connector.getUnauthorisedPaymentsFromPensions(currNino, taxyear).value.futureValue
        assert(result.value === Some(UnauthorisedPaymentsViewModel.empty))
      }

      "return answers" in {
        stubGetAnswers(url, OK, Json.toJson(UnauthorisedPaymentsTestData.answers).toString())
        val result = connector.getUnauthorisedPaymentsFromPensions(currNino, taxyear).value.futureValue
        assert(result.value === Some(UnauthorisedPaymentsTestData.answers))

      }
    }
  }
}
