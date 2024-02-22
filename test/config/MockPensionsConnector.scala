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
import connectors.httpParsers.DeletePensionChargesHttpParser.DeletePensionChargesResponse
import connectors.httpParsers.DeletePensionReliefsHttpParser.DeletePensionReliefsResponse
import connectors.httpParsers.PensionChargesSessionHttpParser.SavePensionChargesAnswersResponse
import connectors.httpParsers.PensionIncomeSessionHttpParser.PensionIncomeSessionResponse
import connectors.httpParsers.PensionReliefsSessionHttpParser.PensionReliefsSessionResponse
import models.APIErrorModel
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import org.scalamock.handlers.{CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import services.{PensionChargesConnectorHelper, PensionIncomeConnectorHelper, PensionReliefsConnectorHelper}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockPensionsConnector extends MockFactory {

  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]
  val mockPensionConnectorHelper               = new PensionChargesConnectorHelper(mockPensionsConnector)
  val mockPensionIncomeConnectorHelper         = new PensionIncomeConnectorHelper(mockPensionsConnector)
  val mockPensionReliefsConnectorHelper        = new PensionReliefsConnectorHelper(mockPensionsConnector)

  def mockSavePensionChargesSessionData(nino: String,
                                        taxYear: Int,
                                        model: CreateUpdatePensionChargesRequestModel,
                                        response: Either[APIErrorModel, Unit]): CallHandler5[
    String,
    Int,
    CreateUpdatePensionChargesRequestModel,
    HeaderCarrier,
    ExecutionContext,
    Future[SavePensionChargesAnswersResponse]] =
    (mockPensionsConnector
      .savePensionCharges(_: String, _: Int, _: CreateUpdatePensionChargesRequestModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockDeletePensionChargesSessionData(
      nino: String,
      taxYear: Int,
      response: Either[APIErrorModel, Unit]): CallHandler4[String, Int, HeaderCarrier, ExecutionContext, Future[DeletePensionChargesResponse]] =
    (mockPensionsConnector
      .deletePensionCharges(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockSavePensionIncomeSessionData(nino: String,
                                       taxYear: Int,
                                       model: CreateUpdatePensionIncomeRequestModel,
                                       response: Either[APIErrorModel, Unit])
      : CallHandler5[String, Int, CreateUpdatePensionIncomeRequestModel, HeaderCarrier, ExecutionContext, Future[PensionIncomeSessionResponse]] =
    (mockPensionsConnector
      .savePensionIncome(_: String, _: Int, _: CreateUpdatePensionIncomeRequestModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockSavePensionReliefSessionData(nino: String, taxYear: Int, model: CreateUpdatePensionReliefsModel, response: Either[APIErrorModel, Unit])
      : CallHandler5[String, Int, CreateUpdatePensionReliefsModel, HeaderCarrier, ExecutionContext, Future[PensionReliefsSessionResponse]] =
    (mockPensionsConnector
      .savePensionReliefs(_: String, _: Int, _: CreateUpdatePensionReliefsModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()

  def mockDeletePensionReliefSessionData(
      nino: String,
      taxYear: Int,
      response: Either[APIErrorModel, Unit]): CallHandler4[String, Int, HeaderCarrier, ExecutionContext, Future[DeletePensionReliefsResponse]] =
    (mockPensionsConnector
      .deletePensionReliefData(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
}
