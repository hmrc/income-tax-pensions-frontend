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
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_FIND_PENSIONS_DATA
import utils.PagerDutyHelper.pagerDutyLog

object StateBenefitsSessionHttpParser extends APIParser {
  type StateBenefitsSessionResponse = Either[APIErrorModel, Unit]

  override val parserName: String = "StateBenefitsSessionHttpParser"
  override val service: String    = "income-tax-state-benefits"

  implicit object StateBenefitsSessionHttpReads extends HttpReads[StateBenefitsSessionResponse] {

    override def read(method: String, url: String, response: HttpResponse): StateBenefitsSessionResponse = {
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
