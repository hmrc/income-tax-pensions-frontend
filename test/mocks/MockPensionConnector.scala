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

import connectors.PensionsConnector
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.CreateUpdatePensionReliefsModel
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait MockPensionConnector extends MockFactory {

  val mockPensionsConnector: PensionsConnector = mock[PensionsConnector]

  object MockPensionConnector {

    def savePensionCharges(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel) =
      (mockPensionsConnector
        .savePensionCharges(_: String, _: Int, _: CreateUpdatePensionChargesRequestModel)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, model, *, *)

    def savePensionReliefs(nino: String, taxYear: Int, model: CreateUpdatePensionReliefsModel) =
      (mockPensionsConnector
        .savePensionReliefs(_: String, _: Int, _: CreateUpdatePensionReliefsModel)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, model, *, *)

    def savePensionIncome(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeRequestModel) =
      (mockPensionsConnector
        .savePensionIncome(_: String, _: Int, _: CreateUpdatePensionIncomeRequestModel)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, model, *, *)

    def deletePensionIncome(nino: String, taxYear: Int) =
      (mockPensionsConnector
        .deletePensionIncome(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, *, *)
  }

}
