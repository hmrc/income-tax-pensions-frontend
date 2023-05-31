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

import connectors.EmploymentConnector
import connectors.httpParsers.EmploymentSessionHttpParser.SessionResponse
import models.APIErrorModel
import models.pension.employmentPensions.CreateUpdateEmploymentRequest
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockEmploymentConnector extends MockFactory {

  val mockEmploymentConnector: EmploymentConnector = mock[EmploymentConnector]

  def mockSaveEmploymentPensionsData(nino: String, taxYear: Int, model: CreateUpdateEmploymentRequest, response: Either[APIErrorModel, Unit])
  : CallHandler5[String, Int, CreateUpdateEmploymentRequest, HeaderCarrier, ExecutionContext, Future[SessionResponse]] = {

    (mockEmploymentConnector.saveEmploymentPensionsData(_: String, _: Int, _: CreateUpdateEmploymentRequest)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, model, *, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
}
