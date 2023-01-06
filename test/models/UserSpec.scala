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

package models

import play.api.mvc.AnyContent
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest

class UserSpec extends UnitTest {

  ".isAgent method" should {

    val request: AuthorisationRequest[AnyContent] =
      new AuthorisationRequest[AnyContent](User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Agent.toString), fakeRequest)
    val agent: AuthorisationRequest[AnyContent] =
      new AuthorisationRequest[AnyContent](User("1234567890", Some("thing"), "AA123456A", sessionId, AffinityGroup.Individual.toString), fakeRequest)

    "return true if an arn exists" in {
      agent.user.isAgent shouldBe true
    }

    "return false if an arn does not exist" in {
      request.user.isAgent shouldBe false
    }
  }

}
