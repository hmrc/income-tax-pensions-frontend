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
import connectors.httpParsers.PensionChargesSessionHttpParser.{PensionChargesSessionHttpReads, PensionChargesSessionResponse}
import connectors.httpParsers.PensionIncomeSessionHttpParser.{PensionIncomeSessionHttpReads, PensionIncomeSessionResponse}
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionsConnector @Inject()(val http: HttpClient,
                                  val appConfig: AppConfig)(implicit ec: ExecutionContext) {


  def savePensionChargesSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PensionChargesSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/$taxYear"
    http.PUT[CreateUpdatePensionChargesRequestModel, PensionChargesSessionResponse](url,
      model)(CreateUpdatePensionChargesRequestModel.format.writes,  PensionChargesSessionHttpReads, hc, ec)
  }

  def savePensionIncomeSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeModel)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PensionIncomeSessionResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-income/session-data/nino/$nino/taxYear/$taxYear"
    http.PUT[CreateUpdatePensionIncomeModel, PensionIncomeSessionResponse](url,
      model)(CreateUpdatePensionIncomeModel.format.writes,  PensionIncomeSessionHttpReads, hc, ec)
  }
}
