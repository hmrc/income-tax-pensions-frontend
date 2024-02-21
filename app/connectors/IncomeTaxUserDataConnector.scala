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
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataHttpReads
import models.IncomeTaxUserData
import models.logging.ConnectorRequestInfo
import play.api.Logging

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxUserDataConnector @Inject() (val http: HttpClient, val config: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  /** Get all the data required by Pensions. Some of the sub-models are taken from income-tax microservices different than pension backend */
  def getUserData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    val incomeTaxUserDataUrl: String = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    ConnectorRequestInfo("GET", incomeTaxUserDataUrl, "income-tax-submission").logRequest(logger)
    http.GET[IncomeTaxUserDataResponse](incomeTaxUserDataUrl).map(copyExternalModelToPension)
  }

  /** It makes sure that any external models we use as a part of our (internal) Pension Model are propagated
    */
  private def copyExternalModelToPension(response: IncomeTaxUserDataResponse) =
    response.map { r =>
      val stateBenefits = r.stateBenefits
      if (stateBenefits.isEmpty && r.pensions.isEmpty) r
      else {
        r.copy(
          pensions = r.pensions.map(_.copy(stateBenefits = stateBenefits))
        )
      }
    }

}
