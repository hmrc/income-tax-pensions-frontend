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
import connectors.httpParsers.IncomeTaxUserDataHttpParser.{IncomeTaxUserDataHttpReads, IncomeTaxUserDataResponse}
import connectors.httpParsers.RefreshIncomeSourceHttpParser.{RefreshIncomeSourceResponse, _}
import models.RefreshIncomeSourceRequest
import models.logging.ConnectorRequestInfo
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxUserDataConnector @Inject() (val http: HttpClient, val config: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def getUserData(nino: String, taxYear: Int)(hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    implicit val headerCarrier: HeaderCarrier = hcWithCorrelationId(hc)

    val incomeTaxUserDataUrl: String = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    ConnectorRequestInfo("GET", incomeTaxUserDataUrl, "income-tax-submission").logRequest(logger)
    http.GET[IncomeTaxUserDataResponse](incomeTaxUserDataUrl)
  }

  def refreshPensionsResponse(nino: String, mtditid: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[RefreshIncomeSourceResponse] =
    refreshPensionsResponse(taxYear, nino)(hc.withExtraHeaders(("mtditid", mtditid)))

  private def refreshPensionsResponse(taxYear: Int, nino: String)(hc: HeaderCarrier): Future[RefreshIncomeSourceResponse] = {
    implicit val headerCarrier: HeaderCarrier = hcWithCorrelationId(hc)

    val url   = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    val model = RefreshIncomeSourceRequest("pensions")
    ConnectorRequestInfo("PUT", url, "income-tax-submission").logRequestWithBody(logger, model)
    http.PUT[RefreshIncomeSourceRequest, RefreshIncomeSourceResponse](url, model)
  }

}
