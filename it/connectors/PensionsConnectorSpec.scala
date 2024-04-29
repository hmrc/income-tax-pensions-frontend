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

import builders.EmploymentPensionsBuilder.anEmploymentPensions
import cats.implicits.catsSyntaxEitherId
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.TaxYear
import models.pension.employmentPensions.EmploymentPensions
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsObject, Json}
import utils.IntegrationTest

class PensionsConnectorSpec extends IntegrationTest {

  "Loading prior employment" when {
    "downstream calls are successful" when {
      "downstream returns a 204" should {
        "return an empty EmploymentPensionsModel" in new Test with EmploymentTest {
          stubDownstreamSuccessResponse(withStatus = NO_CONTENT)

          val result = connector.loadPriorEmployment(nino, TaxYear(taxYear)).futureValue

          result shouldBe EmploymentPensions.empty.asRight
        }
      }
      "downstream returns a 200" should {
        "return a populated EmploymentPensions model" in new Test with EmploymentTest {
          stubDownstreamSuccessResponse(withStatus = OK)

          val result = connector.loadPriorEmployment(nino, TaxYear(taxYear)).futureValue

          result shouldBe anEmploymentPensions.asRight
        }
      }
      "return an APIErrorModel when downstream calls are unsuccessful" in new Test with EmploymentTest {
        stubDownstreamFailureResponse()

        val result = connector.loadPriorEmployment(nino, TaxYear(taxYear)).futureValue

        result shouldBe downstreamError.asLeft
      }
    }

    trait Test {
      val connector: PensionsConnector =
        new PensionsConnector(httpClient, appConfig)

      val downstreamError: APIErrorModel =
        APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)
    }

    trait EmploymentTest {
      _: Test =>
      val downstreamUrl: String =
        s"/income-tax-pensions/employment-pension/nino/$nino/taxYear/$taxYear"

      def stubDownstreamSuccessResponse(withStatus: Int): StubMapping =
        stubGet(
          url = downstreamUrl,
          returnedStatus = withStatus,
          returnedBody = Json.stringify(Json.toJson(anEmploymentPensions))
        )

      def stubDownstreamFailureResponse(): StubMapping =
        stubGet(
          url = downstreamUrl,
          returnedStatus = INTERNAL_SERVER_ERROR,
          returnedBody = Json.stringify(JsObject.empty)
        )
    }
  }
}
