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

package filters

import akka.stream.Materializer
import models.logging.CorrelationId.{RequestHeaderOps, ResultOps}
import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import play.api.mvc._
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class CorrelationIdFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(originalRequestHeader: RequestHeader): Future[Result] = {
    val (updatedRequestHeader, correlationId) = originalRequestHeader.withCorrelationId()

    Mdc
      .withMdc(
        block = nextFilter(updatedRequestHeader).map { result =>
          result.withCorrelationId(correlationId)
        },
        mdcData = Map(CorrelationIdHeaderKey -> correlationId)
      )(ec)
  }

}
