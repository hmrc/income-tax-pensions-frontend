/*
 * Copyright 2022 HM Revenue & Customs
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

package builders

import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import builders.PensionsUserDataBuilder.aPensionsUserData


object UserBuilder {
  val aUserRequest: User = User(
    mtditid = aPensionsUserData.mtdItId,
    arn = None,
    nino = aPensionsUserData.nino,
    sessionId = aPensionsUserData.sessionId,
    affinityGroup = "affinityGroup")

  val aUser: User = User(
    mtditid = aPensionsUserData.mtdItId,
    arn = None,
    nino = aPensionsUserData.nino,
    sessionId = aPensionsUserData.sessionId,
    affinityGroup = "affinityGroup")
}
