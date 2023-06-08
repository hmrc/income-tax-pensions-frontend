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

package connectors.httpParsers

import models.APIErrorModel
import models.pension.employmentPensions.CreateUpdateEmploymentRequest.CreatedEmployment
import play.api.http.Status.CREATED
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EmploymentSessionHttpParser extends APIParser {
  type EmploymentSessionResponse = Either[APIErrorModel, Unit]
  
  override val parserName: String = "EmploymentSessionResponse"
  override val service: String = "income-tax-employment"
  
  implicit object EmploymentSessionHttpReads extends HttpReads[EmploymentSessionResponse] {
    override def read(method: String, url: String, response: HttpResponse): EmploymentSessionResponse =
      response.status match {
        case CREATED => response.json.validate[CreatedEmployment].fold[EmploymentSessionResponse](
          _ => badSuccessJsonFromAPI, _ => Right(()))
        case _ =>
          SessionHttpReads.read(method, url, response)
      }
  }
}
