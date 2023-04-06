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

import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import utils.IntegrationTest

class ExcludeJourneyConnectorISpec extends IntegrationTest {

  val connector: ExcludeJourneyConnector = app.injector.instanceOf[ExcludeJourneyConnector]

  val url = s"/income-tax-submission-service/income-tax/nino/AA123456A/sources/exclude-journey/$taxYear"

  ".excludeJourney" should {

    "return a NoContent" when {

      "the backend returns a response with the status of NO_CONTENT(204)" in {
        val result = {
          stubPost(url, NO_CONTENT, "{}")
          connector.excludeJourney("interest", taxYear, nino)
        }

        await(result) shouldBe Right(NO_CONTENT)
      }

    }

    "return a BadRequest" when {

      "the backend returns a bad request" in {
        val result = {
          stubPost(url, BAD_REQUEST, Json.stringify(Json.obj("code" -> "IT_WRONG", "reason" -> "It bad yoh")))
          connector.excludeJourney("interest", taxYear, nino)
        }

        await(result) shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("IT_WRONG", "It bad yoh")))
      }
    }

    "return an InternalServerError" when {

      "the backend returns an internal server error" in {
        val result = {
          stubPost(url, INTERNAL_SERVER_ERROR, Json.stringify(Json.obj("code" -> "WE_GON_GOOFED", "reason" -> "We messed up, my bad.")))
          connector.excludeJourney("interest", taxYear, nino)
        }

        await(result) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("WE_GON_GOOFED", "We messed up, my bad.")))
      }

      "the backend returns an unexpected answer" in {
        val result = {
          stubPost(url, IM_A_TEAPOT, Json.stringify(Json.obj("code" -> "TEAPOT", "reason" -> "Imma teapot")))
          connector.excludeJourney("interest", taxYear, nino)
        }

        await(result) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("TEAPOT", "Imma teapot")))
      }
    }
  }
}
