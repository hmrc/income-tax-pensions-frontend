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

package models

import models.mongo.ServiceError
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsValue, Json, OFormat}

sealed trait APIErrorBody

case class APIErrorModel(status: Int, body: APIErrorBody) extends ServiceError {
  def toJson: JsValue =
    body match {
      case error: APIErrorBodyModel   => Json.toJson(error)
      case errors: APIErrorsBodyModel => Json.toJson(errors)
    }

  override val message: String = toJson.toString()

  def notFound: Boolean = status == NOT_FOUND
}

/** Single API Error * */
case class APIErrorBodyModel(code: String, reason: String) extends APIErrorBody

object APIErrorBodyModel {
  implicit val formats: OFormat[APIErrorBodyModel]     = Json.format[APIErrorBodyModel]
  val parsingError: APIErrorBodyModel                  = APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
  def dbError(details: String): APIErrorBodyModel      = APIErrorBodyModel("DB_ERROR", s"Error parsing data from database: $details")
  def genericError(details: String): APIErrorBodyModel = APIErrorBodyModel("GENERIC_ERROR", s"Error: $details")
}

/** Multiple API Errors * */
case class APIErrorsBodyModel(failures: Seq[APIErrorBodyModel]) extends APIErrorBody

object APIErrorsBodyModel {
  implicit val formats: OFormat[APIErrorsBodyModel] = Json.format[APIErrorsBodyModel]
}
