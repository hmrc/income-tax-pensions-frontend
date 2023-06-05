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

import connectors.PensionsConnector
import connectors.httpParsers.StateBenefitsSessionHttpParser.{StateBenefitsSessionHttpReads, StateBenefitsSessionResponse}
import connectors.httpParsers.PensionIncomeSessionHttpParser.PensionIncomeSessionResponse
import models.APIErrorModel
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeModel
import models.pension.reliefs.CreateOrUpdatePensionReliefsModel
import org.scalamock.handlers.{CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import connectors.StateBenefitsConnector
import models.mongo.StateBenefitsUserData

trait MockStateBenefitsConnector extends MockFactory {

  val mockStateBenefitsConnector: StateBenefitsConnector = mock[StateBenefitsConnector]

  def mockSaveClaimData(nino: String, model: StateBenefitsUserData, response: Either[APIErrorModel, Unit]):
  CallHandler4[String, StateBenefitsUserData, HeaderCarrier, ExecutionContext, Future[StateBenefitsSessionResponse]] = {
    (mockStateBenefitsConnector.saveClaimData(_: String, _: StateBenefitsUserData)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
}
