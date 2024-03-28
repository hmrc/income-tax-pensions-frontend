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

package connectors.httpParsers

import connectors.DownstreamErrorOr
import models.logging.ConnectorResponseInfo
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_FIND_PENSIONS_DATA
import utils.PagerDutyHelper.pagerDutyLog

object DeleteStateBenefitsHttpParser extends APIParser {
  override val parserName: String = "DeleteStateBenefitsHttpParser"
  override val service: String    = "income-tax-state-benefits"

  implicit object DeleteStateBenefitsHttpReads extends HttpReads[DownstreamErrorOr[Unit]] {

    override def read(method: String, url: String, response: HttpResponse): DownstreamErrorOr[Unit] = {
      ConnectorResponseInfo(method, url, response).logResponseWarnOn4xx(logger)

      response.status match {
        case NOT_FOUND =>
          pagerDutyLog(FAILED_TO_FIND_PENSIONS_DATA, logMessage(response))
          handleAPIError(response)
        case _ => SessionHttpReads.read(method, url, response)
      }
    }
  }
}
