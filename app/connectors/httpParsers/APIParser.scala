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

import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

trait APIParser {

  val parserName : String
  val service : String

  def logMessage(response:HttpResponse): String ={
    s"[$parserName][read] Received ${response.status} from $service API. Body:${response.body}"
  }

  def badSuccessJsonFromAPI[Response]: Either[APIErrorModel, Response] = {
    pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, s"[$parserName][read] Invalid Json from $service API.")
    Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
  }

  def handleAPIError[Response](response: HttpResponse, statusOverride: Option[Int] = None): Either[APIErrorModel, Response] = {

    val status = statusOverride.getOrElse(response.status)

    try {
      val json = response.json

      lazy val apiError = json.asOpt[APIErrorBodyModel]
      lazy val apiErrors = json.asOpt[APIErrorsBodyModel]

      (apiError, apiErrors) match {
        case (Some(apiError), _) => Left(APIErrorModel(status, apiError))
        case (_, Some(apiErrors)) => Left(APIErrorModel(status, apiErrors))
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[$parserName][read] Unexpected Json from $service API.")
          Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
      }
    } catch {
      case _: Exception => Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
    }
  }
  
  type SessionResponse = Either[APIErrorModel, Unit]
  
  implicit object SessionHttpReads extends HttpReads[SessionResponse] {
    override def read(method: String, url: String, response: HttpResponse): SessionResponse = {
      response.status match {
        case NO_CONTENT  => Right(())  //TODO: check NOT_FOUND used in RefreshIncomeSourceHttpReads ONLY
        case BAD_REQUEST =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
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
