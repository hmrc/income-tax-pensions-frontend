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

import uk.gov.hmrc.http.HeaderCarrier

object HeaderCarrierExtensions {
  val CorrelationIdHeaderKey = "X-CorrelationId" // Notice it is not  X-Correlation-Id - we keep exactly the same format as downstream IF/DES
  val MtditIdHeaderKey       = "mtditid"

  implicit class HeaderCarrierOps(val headerCarrier: HeaderCarrier) extends AnyVal {
    def withCorrelationId(correlationId: String): HeaderCarrier =
      addIfMissing(CorrelationIdHeaderKey, correlationId)

    def maybeCorrelationId: Option[String] = headerCarrier.otherHeaders.collectFirst {
      case (key, value) if key == CorrelationIdHeaderKey => value
    }

    def correlationId: String = maybeCorrelationId.getOrElse("unknown")

    def withMtditId(mtditid: String): HeaderCarrier =
      addIfMissing(MtditIdHeaderKey, mtditid)

    private def addIfMissing(headerKey: String, headerValue: String): HeaderCarrier = {
      val extraHeaders = headerCarrier.extraHeaders
      if (headerCarrier.extraHeaders.map(_._1).map(_.toLowerCase()).contains(headerKey.toLowerCase))
        headerCarrier
      else {
        val updatedExtraHeaders = (headerKey, headerValue) :: extraHeaders.toList
        headerCarrier.withExtraHeaders(updatedExtraHeaders: _*)
      }
    }
  }
}
