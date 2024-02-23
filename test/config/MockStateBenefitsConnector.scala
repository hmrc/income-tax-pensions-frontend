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

package config

import connectors.httpParsers.RefreshIncomeSourceHttpParser.RefreshIncomeSourceResponse
import connectors.httpParsers.StateBenefitsSessionHttpParser.StateBenefitsSessionResponse
import connectors.{IncomeTaxUserDataConnector, StateBenefitsConnector}
import models.APIErrorModel
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockStateBenefitsConnector extends MockFactory {

  val mockSubmissionsConnector: IncomeTaxUserDataConnector = mock[IncomeTaxUserDataConnector]

  def mockRefreshPensionsResponse(): CallHandler4[String, String, Int, HeaderCarrier, Future[RefreshIncomeSourceResponse]] =
    (mockSubmissionsConnector
      .refreshPensionsResponse(_: String, _: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returns(Future.successful(Right(())))
      .anyNumberOfTimes()

  val mockStateBenefitsConnector: StateBenefitsConnector = mock[StateBenefitsConnector]

  def mockSaveClaimData(nino: String, model: StateBenefitsUserData, response: Either[APIErrorModel, Unit])
      : CallHandler4[String, StateBenefitsUserData, HeaderCarrier, ExecutionContext, Future[StateBenefitsSessionResponse]] =
    (mockStateBenefitsConnector
      .saveClaimData(_: String, _: StateBenefitsUserData)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
}
