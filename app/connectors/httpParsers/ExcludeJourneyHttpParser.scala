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
import models.logging.ConnectorResponseInfo
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ExcludeJourneyHttpParser extends APIParser {

  type ExcludeJourneyResponse = Either[APIErrorModel, Int]

  override val parserName: String = "ExcludeJourneyHttpParser"
  override val service: String = "income-tax-submission"

  implicit object ExcludeJourneyResponseReads extends HttpReads[ExcludeJourneyResponse] {
    override def read(method: String, url: String, response: HttpResponse): ExcludeJourneyResponse = {
      ConnectorResponseInfo(method, url, response).logResponseWarnOn4xx(logger)

      response.status match  {
        case NO_CONTENT => Right(NO_CONTENT)
        case BAD_REQUEST => handleAPIError(response)
        case INTERNAL_SERVER_ERROR => handleAPIError(response)
        case _ => handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
