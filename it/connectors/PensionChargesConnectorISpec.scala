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

package connectors

import builders.PensionsUserDataBuilder.currentTaxYear
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest
import utils.ModelHelpers.{emptyChargesDownstreamRequestModel, emptyPensionContributions}

class PensionChargesConnectorISpec extends IntegrationTest {

  val hc: HeaderCarrier = headerCarrier.withExtraHeaders("X-Session-ID" -> sessionId)

  val requestModel: CreateUpdatePensionChargesRequestModel =
    emptyChargesDownstreamRequestModel
      .copy(pensionContributions = emptyPensionContributions
        .copy(isAnnualAllowanceReduced = false.some)
        .some)

  val downstreamUrl = s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear"

  val connector = new PensionChargesConnector(httpClient, appConfig)

  "saving journey answers" when {
    "downstream returns a successful result" should {
      "return Unit" in {
        stubPutWithBodyAndHeaders(
          url = downstreamUrl,
          requestBody = requestModel,
          expectedStatus = NO_CONTENT,
          responseBody = JsObject.empty,
          sessionHeader = "X-Session-ID" -> sessionId,
          mtdidHeader = "mtditid"        -> mtditid
        )

        val result = connector.saveAnswers(requestModel, currentTaxYear, nino)(hc).futureValue

        result shouldBe ().asRight
      }
    }
    "downstream call is unsuccessful" should {
      "return an API error" in {
        stubPutWithBodyAndHeaders(
          url = downstreamUrl,
          requestBody = requestModel,
          expectedStatus = INTERNAL_SERVER_ERROR,
          responseBody = JsObject.empty,
          sessionHeader = "X-Session-ID" -> sessionId,
          mtdidHeader = "mtditid"        -> mtditid
        )

        val result = connector.saveAnswers(requestModel, currentTaxYear, nino)(hc).futureValue

        val expectedApiError =
          APIErrorModel(
            status = INTERNAL_SERVER_ERROR,
            body = APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
          )

        result shouldBe expectedApiError.asLeft
      }
    }
  }

}
