
package utils

import builders.UserBuilder.aUser
import common.SessionValues
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

trait FakeRequestProvider {

  protected val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  protected val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  protected val fakeAgentRequest: FakeRequest[AnyContentAsEmpty.type] = fakeIndividualRequest
    .withSession(SessionValues.CLIENT_MTDITID -> aUser.mtditid, SessionValues.CLIENT_NINO -> aUser.nino)
}
