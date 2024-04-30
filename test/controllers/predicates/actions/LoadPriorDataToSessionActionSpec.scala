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
