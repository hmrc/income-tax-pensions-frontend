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
import connectors.httpParsers.DeletePensionChargesHttpParser.{DeletePensionChargesHttpReads, DeletePensionChargesResponse}
import connectors.httpParsers.DeletePensionIncomeHttpParser.{DeletePensionIncomeHttpReads, DeletePensionIncomeResponse}
import connectors.httpParsers.DeletePensionReliefsHttpParser.{DeletePensionReliefsHttpReads, DeletePensionReliefsResponse}
import connectors.httpParsers.PensionChargesSessionHttpParser.{PensionChargesSessionHttpReads, SavePensionChargesAnswersResponse}
import connectors.httpParsers.PensionIncomeSessionHttpParser.{PensionIncomeSessionHttpReads, PensionIncomeSessionResponse}
import connectors.httpParsers.PensionReliefsSessionHttpParser.{PensionReliefsSessionHttpReads, PensionReliefsSessionResponse}
import models.logging.ConnectorRequestInfo
import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends Logging {

  def savePensionCharges(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[SavePensionChargesAnswersResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionChargesRequestModel, SavePensionChargesAnswersResponse](url, model)(
      CreateUpdatePensionChargesRequestModel.format,
      PensionChargesSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionCharges(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionChargesResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionChargesResponse](url)(DeletePensionChargesHttpReads, hcWithCorrelationId(hc), ec)
  }

  def savePensionIncome(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[PensionIncomeSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionIncomeRequestModel, PensionIncomeSessionResponse](url, model)(
      CreateUpdatePensionIncomeRequestModel.writes,
      PensionIncomeSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionIncome(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionIncomeResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionIncomeResponse](url)(DeletePensionIncomeHttpReads, hc, ec)
  }

  def savePensionReliefs(nino: String, taxYear: Int, model: CreateUpdatePensionReliefsModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[PensionReliefsSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionReliefsModel, PensionReliefsSessionResponse](url, model)(
      CreateUpdatePensionReliefsModel.format,
      PensionReliefsSessionHttpReads,
      hcWithCorrelationId(hc),
      ec
    )
  }

  def deletePensionReliefData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionReliefsResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionReliefsResponse](url)(DeletePensionReliefsHttpReads, hcWithCorrelationId(hc), ec)
  }
}
