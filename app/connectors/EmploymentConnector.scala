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
import connectors.Connector.hcWithCorrelationId
import connectors.httpParsers.DeleteEmploymentHttpParser.DeleteEmploymentHttpReads
import connectors.httpParsers.SaveEmploymentHttpParser.SaveEmploymentHttpReads
import models.logging.ConnectorRequestInfo
import models.pension.employmentPensions.CreateUpdateEmploymentRequest
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmploymentConnector @Inject() (val http: HttpClient, val config: AppConfig) extends Logging {
  def saveEmployment(nino: String, taxYear: Int, model: CreateUpdateEmploymentRequest)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {

    val url = s"${config.employmentBEBaseUrl}/income-tax/nino/$nino/sources?taxYear=$taxYear"
    ConnectorRequestInfo("POST", url, "income-tax-employment").logRequestWithBody(logger, model)

    http.POST[CreateUpdateEmploymentRequest, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdateEmploymentRequest.format,
      SaveEmploymentHttpReads,
      hcWithCorrelationId,
      ec)
  }

  def deleteEmployment(nino: String, taxYear: Int, employmentId: String)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    // TODO: Will there ever be HMRC-held employments?
    val source = "CUSTOMER"
    val url    = s"${config.employmentBEBaseUrl}/income-tax/nino/$nino/sources/$employmentId/$source?taxYear=$taxYear"

    ConnectorRequestInfo("DELETE", url, "income-tax-employment").logRequest(logger)

    http.DELETE[DownstreamErrorOr[Unit]](url)(
      DeleteEmploymentHttpReads,
      hcWithCorrelationId,
      ec
    )
  }
}
