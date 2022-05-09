/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.httpParsers

import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataHttpReads
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import models.pension.{EmploymentPensionModel, EmploymentPensions}
import uk.gov.hmrc.http.HttpResponse
import play.api.http.Status
import play.api.libs.json.Json
import utils.{PensionDataStubs, UnitTest}

class IncomeTaxUserDataHttpParserISpec extends UnitTest {

  "The IncomeTaxUserDataHttpParser" when {

    val userDataJson: String = {
      s"""
         |{
         |  "pensions": ${Json.toJson(PensionDataStubs.fullPensionsModel)},
         |  "employment": ${PensionDataStubs.employmentPensionsJson}
         |}
         |""".stripMargin
    }

    "the response status is OK and both pensions and employment sections are returned" should {

      "filter out any occPen items that aren't true in employment and parse the model correctly" in {

        val expectedResult =
          IncomeTaxUserData(
            pensions = Some(PensionDataStubs.fullPensionsModel),
            employment = Some(
              EmploymentPensions(
                Seq(
                  EmploymentPensionModel(
                    "1234567890",
                    "HMRC pensions scheme",
                    Some("Some HMRC ref"),
                    Some("Payroll ID"),
                    Some("very early"),
                    Some("too late"),
                    Some(BigDecimal(127000)),
                    Some(BigDecimal(450)),
                    Some(true))
                ),
                Seq()
              )
            )
          )

        IncomeTaxUserDataHttpReads.read("", "",
          HttpResponse(Status.OK, s"$userDataJson")) shouldBe Right(expectedResult)
      }
    }

    "the response status is NO_CONTENT" should {

      "return an empty UncomeTaxUserData model" in {

        IncomeTaxUserDataHttpReads.read("", "",
          HttpResponse(Status.NO_CONTENT, s"$userDataJson")) shouldBe Right(IncomeTaxUserData())
      }
    }

    "the response status is INTERNAL_SERVER_ERROR" should {

      "return an error" in {

        val expectedError = APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
        val expectedResult = Json.toJson(expectedError)

        IncomeTaxUserDataHttpReads.read("", "",
          HttpResponse(Status.INTERNAL_SERVER_ERROR, s"$expectedResult")
        ) shouldBe Left(APIErrorModel(Status.INTERNAL_SERVER_ERROR, expectedError))
      }
    }

    "the response status is SERVICE_UNAVAILABLE" should {

      "return an error" in {

        val expectedError = APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
        val expectedResult = Json.toJson(expectedError)

        IncomeTaxUserDataHttpReads.read("", "",
          HttpResponse(Status.SERVICE_UNAVAILABLE, s"$expectedResult")
        ) shouldBe Left(APIErrorModel(Status.SERVICE_UNAVAILABLE, expectedError))
      }
    }

    "the response status is not handled specifically" should {

      "return an internal server error" in {

        IncomeTaxUserDataHttpReads.read("", "",
          HttpResponse(Status.IM_A_TEAPOT, "")
        ) shouldBe Left(APIErrorModel(Status.INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
