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

package connectors

import config.AppConfig
import connectors.httpParsers.EmploymentSessionHttpParser.{EmploymentSessionHttpReads, EmploymentSessionResponse}
import models.pension.employmentPensions.CreateUpdateEmploymentRequest
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentConnector @Inject()(val http: HttpClient,
                                    val config: AppConfig) extends Logging {
  def saveEmploymentPensionsData(nino: String, taxYear: Int, model: CreateUpdateEmploymentRequest)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmploymentSessionResponse] = {
    
    val url = s"${config.employmentBEBaseUrl}/income-tax/nino/$nino/sources?taxYear=$taxYear"
    logger.debug(s"Call from Connector, body: ${CreateUpdateEmploymentRequest.format.writes(model)}")

    http.POST[CreateUpdateEmploymentRequest, EmploymentSessionResponse](url,model)(
      CreateUpdateEmploymentRequest.format.writes, EmploymentSessionHttpReads, hc, ec)
  }
}
