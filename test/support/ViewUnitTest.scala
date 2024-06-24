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

package support

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.AppConfig
import models.AuthorisationRequest
import models.requests.UserSessionDataRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContent
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.{FakeRequestProvider, TestTaxYearHelper}

trait ViewUnitTest
    extends UnitTest
    with UserScenarios
    with ViewHelper
    with GuiceOneAppPerSuite
    with Injecting
    with PageUrlsHelpers
    with TestTaxYearHelper
    with FakeRequestProvider {

  // private val fakeRequest = FakeRequest().withHeaders("X-Session-ID" -> aUser.sessionId)

  protected implicit val mockAppConfig: AppConfig      = app.injector.instanceOf[AppConfig]
  protected implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]

  protected lazy val defaultMessages: Messages = messagesApi.preferred(fakeRequest.withHeaders())
  protected lazy val welshMessages: Messages   = messagesApi.preferred(Seq(Lang("cy")))

  protected lazy val individualUserRequest = new AuthorisationRequest[AnyContent](aUser, fakeRequest)
  protected lazy val agentUserRequest =
    new AuthorisationRequest[AnyContent](aUser.copy(arn = Some("arn"), affinityGroup = AffinityGroup.Agent.toString), fakeRequest)

  protected lazy val individualUserDataRequest: UserSessionDataRequest[AnyContent] = new UserSessionDataRequest(aPensionsUserData, aUser, fakeRequest)
  protected lazy val agentUserDataRequest: UserSessionDataRequest[AnyContent] =
    new UserSessionDataRequest(aPensionsUserData, aUser.copy(arn = Some("arn"), affinityGroup = AffinityGroup.Agent.toString), fakeRequest)

  protected def getMessages(isWelsh: Boolean): Messages = if (isWelsh) welshMessages else defaultMessages

  protected def getAuthRequest(isAgent: Boolean): AuthorisationRequest[AnyContent] =
    if (isAgent) agentUserRequest else individualUserRequest

  protected def getUserSession(isAgent: Boolean): UserSessionDataRequest[AnyContent] =
    if (isAgent) agentUserDataRequest else individualUserDataRequest
}
