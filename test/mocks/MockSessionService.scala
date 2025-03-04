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

import common.TaxYear
import models.IncomeTaxUserData.PriorData
import models.User
import models.mongo.PensionsUserData.SessionData
import org.scalamock.handlers.{CallHandler2, CallHandler4}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.PensionsUserDataRepository.QueryResult
import services.{PensionSessionService, ServiceOutcomeT}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.ExecutionContext

trait MockSessionService extends MockFactory with TestSuite {

  val mockSessionService: PensionSessionService = mock[PensionSessionService]

  object MockSessionService {

    def loadSession(
                     taxYear: Int,
                     user: User
                   ): CallHandler2[Int, User, QueryResult[Option[SessionData]]] =
      (mockSessionService
        .loadSession(_: Int, _: User))
        .expects(*, *)

    def loadPriorAndSession(
                             user: User,
                             taxYear: TaxYear
                           ): CallHandler4[User, TaxYear, HeaderCarrier, ExecutionContext, ServiceOutcomeT[(PriorData, SessionData)]] =
      (mockSessionService
        .loadPriorAndSession(_: User, _: TaxYear)(_: HeaderCarrier, _: ExecutionContext))
        .expects(user, taxYear, *, *)
  }
}
