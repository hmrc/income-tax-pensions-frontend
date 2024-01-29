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

import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import connectors.httpParsers.EmploymentSessionHttpParser.EmploymentSessionResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class EmploymentConnectorISpec extends IntegrationTest {

  lazy val connector: EmploymentConnector            = app.injector.instanceOf[EmploymentConnector]
  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  "EmploymentConnector" should {

    "Return a success result" when {
      "submission returns a 204" in new Setup {
        val httpStatus = NO_CONTENT
        saveData() shouldBe successSaveResponse
      }
    }

    "Return an error result" when {
      for (status <- Seq(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE))
        s"submission returns a $status " in new Setup {
          val httpStatus = status
          saveData() shouldBe failedSaveResponse
        }
    }

    trait Setup {
      val httpStatus: Int

      val successSaveResponse     = Right(())
      lazy val failedSaveResponse = Left(APIErrorModel(httpStatus, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))

      def saveData(): EmploymentSessionResponse = {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
          .withExtraHeaders("mtditid" -> mtditid)

        val payload = Json
          .toJson(
            aPensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
              .map(_.toCreateUpdateEmploymentRequest)
              .head)
          .toString()

        stubPostWithHeadersCheck(
          s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          httpStatus,
          payload,
          "{}",
          "X-Session-ID" -> sessionId,
          "mtditid"      -> mtditid)

        val sessionUserData =
          aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel.copy(incomeFromPensions = aPensionsUserData.pensions.incomeFromPensions))

        val model = sessionUserData.pensions.incomeFromPensions.uKPensionIncomes.map(_.toCreateUpdateEmploymentRequest).head
        Await.result(connector.saveEmploymentPensionsData(nino, taxYear, model)(hc, ec), Duration.Inf)
      }
    }
  }
}
