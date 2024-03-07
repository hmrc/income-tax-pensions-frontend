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

import common.TaxYear
import models.User
import org.scalamock.scalatest.MockFactory
import services.PensionSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait MockSessionService extends MockFactory {

  val mockSessionService: PensionSessionService = mock[PensionSessionService]

  object MockSessionService {

    def loadPriorAndSession(user: User, taxYear: TaxYear) =
      (mockSessionService
        .loadPriorAndSession(_: User, _: TaxYear)(_: HeaderCarrier, _: ExecutionContext))
        .expects(user, taxYear, *, *)
  }
}
