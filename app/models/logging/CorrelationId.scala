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

package models.logging

import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.http.HttpResponse

import java.util.UUID

object CorrelationId {
  implicit class RequestHeaderOps(val value: RequestHeader) extends AnyVal {

    def withCorrelationId(): (RequestHeader, String) = {
      val correlationId = value.headers
        .get(CorrelationIdHeaderKey)
        .getOrElse(CorrelationId.generate())

      val updatedRequest =
        if (value.headers.hasHeader(CorrelationIdHeaderKey)) value
        else value.withHeaders(value.headers.add(CorrelationIdHeaderKey -> correlationId))

      (updatedRequest, correlationId)
    }

  }

  implicit class ResultOps(val value: Result) extends AnyVal {
    def withCorrelationId(correlationId: String): Result =
      if (value.header.headers.contains(CorrelationIdHeaderKey)) value
      else value.withHeaders(CorrelationIdHeaderKey -> correlationId)
  }

  implicit class HttpResponseOps(val value: HttpResponse) extends AnyVal {
    def correlationId: String =
      value.header(CorrelationIdHeaderKey).getOrElse("unknown")

  }

  private def generate(): String = UUID.randomUUID().toString
}
