package services

import builders.PensionsUserDataBuilder.{aPensionsUserData, user}
import config.MockPensionsConnector
import models.pension.Journey
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import support.mocks.MockPensionSessionService
import utils.CommonData._

class PensionsServiceImplSpec extends AnyWordSpecLike with MockPensionsConnector with MockPensionSessionService {
  val service = new PensionsServiceImpl(mockPensionsConnector, mockPensionSessionService)

  "upsertPaymentsIntoPensions" should {
    "remove answers after submission" in {
      val session = aPensionsUserData
      mockSavePaymentsIntoPensions()
      mockClearSessionOnSuccess(Journey.PaymentsIntoPensions)
      val result = service.upsertPaymentsIntoPensions(user, currTaxYear, session).value.futureValue

      assert(result.isRight === true)
    }
  }
}
