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

package connectors

import common.TaxYear
import config.AppConfig
import connectors.httpParsers.PensionChargesSessionHttpParser.{PensionChargesSessionHttpReads, SavePensionChargesAnswersResponse}
import models.logging.ConnectorRequestInfo
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionChargesConnector @Inject() (val http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends Logging
    with JourneyConnector[Future, CreateUpdatePensionChargesRequestModel, SavePensionChargesAnswersResponse] {

  override def saveAnswers(model: CreateUpdatePensionChargesRequestModel, taxYear: TaxYear, nino: String)(implicit
      hc: HeaderCarrier): Future[SavePensionChargesAnswersResponse] = {
    val url = appConfig.pensionBEBaseUrl + s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.endYear}"
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionChargesRequestModel, SavePensionChargesAnswersResponse](url, model)(
      CreateUpdatePensionChargesRequestModel.format,
      PensionChargesSessionHttpReads,
      hc,
      ec)
  }
}
