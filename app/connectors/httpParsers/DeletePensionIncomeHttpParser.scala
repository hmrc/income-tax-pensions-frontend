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
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object DeletePensionIncomeHttpParser extends APIParser {
  override val parserName: String = "DeletePensionIncomeHttpParser"
  override val service: String    = "income-tax-pensions-frontend"

  implicit object DeletePensionIncomeHttpReads extends HttpReads[DownstreamErrorOr[Unit]] {

    override def read(method: String, url: String, response: HttpResponse): DownstreamErrorOr[Unit] = {
      ConnectorResponseInfo(method, url, response).logResponseWarnOn4xx(logger)

      SessionHttpReads.read(method, url, response)
    }
  }
}
