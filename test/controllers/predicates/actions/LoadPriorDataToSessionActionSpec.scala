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

package controllers.predicates.actions

import builders.PensionsUserDataBuilder.aPensionsUserData
import models.pension.Journey
import models.requests.UserSessionDataRequest
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import support.mocks.MockPensionSessionService
import utils.CommonData.currTaxYear
import utils.UnitTest
import org.scalatest.EitherValues._

class LoadPriorDataToSessionActionSpec extends UnitTest with MockPensionSessionService {
  val action = LoadPriorDataToSessionAction(currTaxYear, Journey.PaymentsIntoPensions, mockPensionSessionService, mockErrorHandler)

  "refine" should {
    "return unchanged request when session data already exist for that journey" in {
      val input  = UserSessionDataRequest(aPensionsUserData, user, fakeRequest)
      val result = action.refine(input).futureValue
      assert(result.value === input)
    }
  }
}
