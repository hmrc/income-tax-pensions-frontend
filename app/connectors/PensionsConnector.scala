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
import connectors.httpParsers.DeletePensionChargesHttpParser.{DeletePensionChargesHttpReads, DeletePensionChargesResponse}
import connectors.httpParsers.DeletePensionIncomeHttpParser.{DeletePensionIncomeHttpReads, DeletePensionIncomeResponse}
import connectors.httpParsers.DeletePensionReliefsHttpParser.{DeletePensionReliefsHttpReads, DeletePensionReliefsResponse}
import connectors.httpParsers.PensionChargesSessionHttpParser.{PensionChargesSessionHttpReads, PensionChargesSessionResponse}
import connectors.httpParsers.PensionIncomeSessionHttpParser.{PensionIncomeSessionHttpReads, PensionIncomeSessionResponse}
import connectors.httpParsers.PensionReliefsSessionHttpParser.{PensionReliefsSessionHttpReads, PensionReliefsSessionResponse}
import models.logging.ConnectorRequestInfo
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeModel
import models.pension.reliefs.CreateOrUpdatePensionReliefsModel
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends Logging {

  def savePensionChargesSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[PensionChargesSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionChargesRequestModel, PensionChargesSessionResponse](url, model)(
      CreateUpdatePensionChargesRequestModel.format,
      PensionChargesSessionHttpReads,
      hc,
      ec)
  }

  def deletePensionCharges(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionChargesResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionChargesResponse](url)(DeletePensionChargesHttpReads, hc, ec)
  }

  def savePensionIncomeSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[PensionIncomeSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionIncomeModel, PensionIncomeSessionResponse](url, model)(
      CreateUpdatePensionIncomeModel.writes,
      PensionIncomeSessionHttpReads,
      hc,
      ec)
  }

  def deletePensionIncomeData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionIncomeResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionIncomeResponse](url)(DeletePensionIncomeHttpReads, hc, ec)
  }

  def savePensionReliefSessionData(nino: String, taxYear: Int, model: CreateOrUpdatePensionReliefsModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[PensionReliefsSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateOrUpdatePensionReliefsModel, PensionReliefsSessionResponse](url, model)(
      CreateOrUpdatePensionReliefsModel.format,
      PensionReliefsSessionHttpReads,
      hc,
      ec)
  }

  def deletePensionReliefData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeletePensionReliefsResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-reliefs/session-data/nino/$nino/taxYear/${taxYear.toString}"
    ConnectorRequestInfo("DELETE", url, "income-tax-pensions").logRequest(logger)
    http.DELETE[DeletePensionReliefsResponse](url)(DeletePensionReliefsHttpReads, hc, ec)
  }
}
