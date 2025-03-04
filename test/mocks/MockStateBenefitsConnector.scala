/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.StateBenefitsConnector
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers.{CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.ExecutionContext

trait MockStateBenefitsConnector extends MockFactory with TestSuite  {

  val mockStateBenefitsConnector: StateBenefitsConnector = mock[StateBenefitsConnector]

  object MockStateBenefitsConnector {

    def saveClaim(nino: String): CallHandler4[String, StateBenefitsUserData, HeaderCarrier, ExecutionContext, connectors.DownstreamOutcome[Unit]] =
      (mockStateBenefitsConnector
        .saveClaim(_: String, _: StateBenefitsUserData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *, *)

    def deleteClaim(nino: String,
                    taxYear: Int,
                    benefitId: UUID): CallHandler5[String, Int, UUID, HeaderCarrier, ExecutionContext, connectors.DownstreamOutcome[Unit]] =
      (mockStateBenefitsConnector
        .deleteClaim(_: String, _: Int, _: UUID)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear, benefitId, *, *)

  }
}
