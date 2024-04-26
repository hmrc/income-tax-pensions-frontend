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

import cats.implicits.catsSyntaxEitherId
import connectors.DownstreamErrorOr
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.{getCorrelationId, pagerDutyLog}

import scala.util.Try

trait APIParser extends Logging {

  val parserName: String
  val service: String

  def logMessage(response: HttpResponse): String =
    s"[$parserName][read] Received ${response.status} from $parserName. Body:${response.body} ${getCorrelationId(response)}"

  def badSuccessJsonFromAPI[Response]: Either[APIErrorModel, Response] = {
    pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, s"[$parserName][read] Invalid Json from $service API.")
    Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
  }

  def handleAPIError[Response](response: HttpResponse, statusOverride: Option[Int] = None): DownstreamErrorOr[Response] = {

    val status = statusOverride.getOrElse(response.status)

    try {
      val json = response.json

      lazy val apiError  = json.asOpt[APIErrorBodyModel]
      lazy val apiErrors = json.asOpt[APIErrorsBodyModel]

      (apiError, apiErrors) match {
        case (Some(apiError), _)  => Left(APIErrorModel(status, apiError))
        case (_, Some(apiErrors)) => Left(APIErrorModel(status, apiErrors))
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[$parserName][read] Unexpected Json from $service API.")
          Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
      }
    } catch {
      case _: Exception => Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
    }
  }

  def handleError[Response](response: HttpResponse, statusOverride: Option[Int] = None): APIErrorModel = {

    val status = statusOverride.getOrElse(response.status)

    try {
      val json = response.json

      lazy val apiError  = json.asOpt[APIErrorBodyModel]
      lazy val apiErrors = json.asOpt[APIErrorsBodyModel]

      (apiError, apiErrors) match {
        case (Some(apiError), _)  => APIErrorModel(status, apiError)
        case (_, Some(apiErrors)) => APIErrorModel(status, apiErrors)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[$parserName][read] Unexpected Json from $service API.")
          APIErrorModel(status, APIErrorBodyModel.parsingError)
      }
    } catch {
      case _: Exception => APIErrorModel(status, APIErrorBodyModel.parsingError)
    }
  }

  implicit object SessionHttpReads extends HttpReads[DownstreamErrorOr[Unit]] {
    override def read(method: String, url: String, response: HttpResponse): DownstreamErrorOr[Unit] =
      response.status match {
        case NO_CONTENT => ().asRight

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

object APIParser {
  def handleError(response: HttpResponse, statusOverride: Option[Int] = None): APIErrorModel = {
    val status    = statusOverride.getOrElse(response.status)
    val maybeJson = Try(response.json)
    val maybeApiJsonError = maybeJson.map { json =>
      lazy val apiError  = json.asOpt[APIErrorBodyModel]
      lazy val apiErrors = json.asOpt[APIErrorsBodyModel]

      val errorModel = (apiError, apiErrors) match {
        case (Some(apiError), _)  => APIErrorModel(status, apiError)
        case (_, Some(apiErrors)) => APIErrorModel(status, apiErrors)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"Unexpected Json from API")
          APIErrorModel(status, APIErrorBodyModel.parsingError)
      }

      errorModel
    }

    maybeApiJsonError.getOrElse(
      APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", s"Error parsing response from API: ${response.body}"))
    )
  }

  def logMessage(response: HttpResponse): String = s"Received ${response.status}. Body:${response.body} ${getCorrelationId(response)}"

  def unsafePagerDutyError(method: String, url: String, response: HttpResponse): APIErrorModel =
    response.status match {
      case NO_CONTENT =>
        ()
        handleError(response)
      case BAD_REQUEST =>
        pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
        handleError(response)
      case INTERNAL_SERVER_ERROR =>
        pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
        handleError(response)
      case SERVICE_UNAVAILABLE =>
        pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
        handleError(response)
      case _ =>
        pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
        handleError(response, Some(INTERNAL_SERVER_ERROR))
    }
}
