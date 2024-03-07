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
import connectors.httpParsers.DeletePensionChargesHttpParser.DeletePensionChargesHttpReads
import connectors.httpParsers.DeletePensionIncomeHttpParser.DeletePensionIncomeHttpReads
import connectors.httpParsers.DeletePensionReliefsHttpParser.DeletePensionReliefsHttpReads
import connectors.httpParsers.PensionChargesSessionHttpParser.PensionChargesSessionHttpReads
import connectors.httpParsers.PensionIncomeSessionHttpParser.PensionIncomeSessionHttpReads
import connectors.httpParsers.PensionReliefsSessionHttpParser.PensionReliefsSessionHttpReads
import models.logging.ConnectorRequestInfo
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import play.api.Logging
import services.DownstreamOutcome
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PensionsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends Logging {

  def savePensionCharges(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionChargesRequestModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionChargesRequestModel.format,
      PensionChargesSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionCharges(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionChargesHttpReads, hcWithCorrelationId(hc), ec)
  }

  def savePensionIncome(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionIncomeRequestModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionIncomeRequestModel.writes,
      PensionIncomeSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionIncome(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionIncomeHttpReads, hc, ec)
  }

  def savePensionReliefs(nino: String, taxYear: Int, model: CreateUpdatePensionReliefsModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionReliefsModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionReliefsModel.format,
      PensionReliefsSessionHttpReads,
      hcWithCorrelationId(hc),
      ec
    )
  }

  def deletePensionReliefData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionReliefsHttpReads, hcWithCorrelationId(hc), ec)
  }
}
