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

import connectors.DownstreamErrorOr
import models.logging.ConnectorResponseInfo
import models.pension.JourneyNameAndStatus
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{INTERNAL_SERVER_ERROR_FROM_API, SERVICE_UNAVAILABLE_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.pagerDutyLog

object GetAllJourneyStatusesHttpParser extends APIParser {
  override val parserName: String = "GetAllJourneyStatusesHttpParser"
  override val service: String    = "income-tax-pensions-frontend"

  implicit object GetAllJourneyStatusesHttpReads extends HttpReads[DownstreamErrorOr[List[JourneyNameAndStatus]]] {

    override def read(method: String, url: String, response: HttpResponse): DownstreamErrorOr[List[JourneyNameAndStatus]] = {
      ConnectorResponseInfo(method, url, response).logResponseWarnOn4xx(logger)

      response.status match {
        case OK =>
          response.json
            .validate[List[JourneyNameAndStatus]]
            .fold[DownstreamErrorOr[List[JourneyNameAndStatus]]](
              _ => badSuccessJsonFromAPI,
              parsedModel => Right(parsedModel)
            )
        case NO_CONTENT => Right(List[JourneyNameAndStatus]())
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
