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

package mocks

import connectors.EmploymentConnector
import models.pension.employmentPensions.CreateUpdateEmploymentRequest
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait MockEmploymentConnector extends MockFactory {

  val mockEmploymentConnector: EmploymentConnector = mock[EmploymentConnector]

  object MockEmploymentConnector {

    def saveEmployment(nino: String, taxYear: Int) =
      (mockEmploymentConnector
        .saveEmployment(_: String, _: Int, _: CreateUpdateEmploymentRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, *, *, *)

    def deleteEmployment(nino: String, taxYear: Int, employmentId: String) =
      (mockEmploymentConnector
        .deleteEmployment(_: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, employmentId, *, *)

  }

}
