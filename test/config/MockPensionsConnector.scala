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
import connectors.httpParsers.PensionChargesSessionHttpParser.PensionChargesSessionResponse
import connectors.httpParsers.PensionIncomeSessionHttpParser.PensionIncomeSessionResponse
import models.APIErrorModel
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeModel
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockPensionsConnector extends MockFactory {

  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]


  def mockSavePensionChargesSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel, response: Either[APIErrorModel, Unit]):
  CallHandler5[String, Int, CreateUpdatePensionChargesRequestModel, HeaderCarrier, ExecutionContext, Future[PensionChargesSessionResponse]] = {
    (mockPensionsConnector.savePensionChargesSessionData(_: String, _: Int, _: CreateUpdatePensionChargesRequestModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }

  def mockSavePensionIncomeSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeModel, response: Either[APIErrorModel, Unit]):
  CallHandler5[String, Int, CreateUpdatePensionIncomeModel, HeaderCarrier, ExecutionContext, Future[PensionIncomeSessionResponse]]
  = {
    (mockPensionsConnector.savePensionIncomeSessionData(_: String, _: Int, _: CreateUpdatePensionIncomeModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
}
