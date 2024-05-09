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

package services

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder.aPensionsUserData
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import config._
import models.mongo.JourneyStatus.Completed
import models.mongo._
import models.pension.AllPensionsData.generateSessionModelFromPrior
import models.pension.Journey.{PaymentsIntoPensions, PensionsSummary}
import models.pension.JourneyNameAndStatus
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.Injecting
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class PensionSessionServiceSpec
    extends UnitTest
    with MockPensionUserDataRepository
    with MockIncomeTaxUserDataConnector
    with MockPensionsConnector
    with ScalaFutures
    with Injecting {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate   = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate                       = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi                             = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(
    mockFrontendAppConfig)

  implicit val messages: Messages = inject[MessagesApi].preferred(fakeRequest.withHeaders())

  val service: PensionSessionService =
    new PensionSessionService(mockPensionUserDataRepository, mockUserDataConnector, mockPensionsConnector, errorHandler)(mockExecutionContext)

  "loadDataAndHandle" should {
    "invoke block if no errors retrieving session and prior data" in {
      mockFind(taxYear, user, Right(None))
      mockGetAllJourneyStatuses(currentTaxYear, Right(List.empty))
      mockFindNoContent(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)(block = (_, _, _) => Future(Ok))

      status(response) shouldBe OK
    }
    "return a 500 if an error loading prior data" in {
      mockFind(taxYear, user, Right(None))
      mockFindFail(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)((_, _, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 if failed to load session data" in {
      mockFind(taxYear, user, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)((_, _, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }
  "loadPriorAndSession" should {
    "load both prior and session data when retrieval cals are successful" in {
      val prior   = IncomeTaxUserData(pensions = anAllPensionsData.some)
      val session = aPensionsUserData

      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, prior)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, session.some.asRight)

      val result = service.loadPriorAndSession(user, currentTaxYear).value.futureValue

      result shouldBe (prior, session).asRight
    }

    "return a MongoError when there's an error interacting with the database" in {
      val mongoError = MongoError("some error")

      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, IncomeTaxUserData(None))
      super[MockPensionUserDataRepository].mockFind(taxYear, user, mongoError.asLeft)

      val result = service.loadPriorAndSession(user, currentTaxYear).value.futureValue

      result shouldBe mongoError.asLeft
    }

    "return SessionNotFound when no user session is found in the database" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, IncomeTaxUserData(None))
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(None))

      val result = service.loadPriorAndSession(user, currentTaxYear).value.futureValue

      result shouldBe SessionNotFound.asLeft
    }

    "return an APIErrorModel when there's an error loading prior data from downstream" in {
      super[MockIncomeTaxUserDataConnector].mockFindFail(nino, taxYear)

      val result = service.loadPriorAndSession(user, currentTaxYear).value.futureValue

      result shouldBe apiError.asLeft
    }
  }

  // TODO need to investigate and fix how changing joda time to java time has affected this test
  ".createOrUpdateSessionData" should {
    "return SEE_OTHER(303) status when createOrUpdate succeeds" ignore {
      mockCreateOrUpdate(emptySessionData, Right(()))
      val response = service.createOrUpdateSessionData(user, emptyPensionsData, taxYear, isPriorSubmission = true)(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" ignore {
      mockCreateOrUpdate(emptySessionData, Left(DataNotUpdated))
      val response: Future[Result] =
        service.createOrUpdateSessionData(user, emptyPensionsData, taxYear, isPriorSubmission = true)(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  "generateCyaFromPrior" should {
    "generate a PensionsCYAModel from prior AllPensionsData" in {
      mockCreateOrUpdate(emptySessionData, Right(()))
      val response = generateSessionModelFromPrior(anAllPensionDataEmpty)
      response shouldBe aPensionsCYAGeneratedFromPriorEmpty.copy(paymentsIntoPension = PaymentsIntoPensionsViewModel.empty)
    }
  }

  ".createOrUpdateSessionData" should {
    "return Right(unit) when createOrUpdate succeeds" in {
      mockCreateOrUpdate(emptySessionData, Right(()))
      val response = await(service.createOrUpdateSession(emptySessionData))
      response shouldBe Right(())
    }

    "return Left DB Error(400) when createOrUpdate fails" in {
      mockCreateOrUpdate(emptySessionData, Left(DataNotUpdated))
      val response = await(service.createOrUpdateSession(emptySessionData))
      response shouldBe Left(DataNotUpdated)
    }
  }

  "mergePriorDataToSession" should {
    val noPriorData = IncomeTaxUserData(None)

    "return a successful result if the model required updating (session != prior)" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(None))
      mockGetAllJourneyStatuses(currentTaxYear, Right(List.empty))
      mockCreateOrUpdate(Right(()))

      val response = service.mergePriorDataToSession(PensionsSummary, taxYear, user, (_, _) => Ok)

      assert(status(response) === OK)
    }

    "return a successful if not update required (session == prior)" in {
      val sessionWithEmptyModel = aPensionsUserData.copy(pensions = PensionsCYAModel.emptyModels)
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(Some(sessionWithEmptyModel)))
      mockGetAllJourneyStatuses(currentTaxYear, Right(List.empty))
      mockCreateOrUpdate(Right(()))

      val response = service.mergePriorDataToSession(PensionsSummary, taxYear, user, (_, _) => Ok)

      assert(status(response) === OK)
    }

    "return an internal server error if data not found" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Left(DataNotFound))

      val response = service.mergePriorDataToSession(PensionsSummary, taxYear, user, (_, _) => Ok)

      assert(status(response) === INTERNAL_SERVER_ERROR)
    }

    "return an internal server error if data not updated" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(None))
      mockGetAllJourneyStatuses(currentTaxYear, Right(List.empty))
      mockCreateOrUpdate(Left(DataNotUpdated))

      val response = service.mergePriorDataToSession(PensionsSummary, taxYear, user, (_, _) => Ok)

      assert(status(response) === INTERNAL_SERVER_ERROR)
    }

  }

  "getJourneyStatus" should {
    val ctx = JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions)

    "return a successful result" which {
      "contains a None when an empty list is returned from the database" in {
        mockGetJourneyStatus(ctx, Right(List()))

        val response = service.getJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions))

        await(response) shouldBe Right(None)
      }

      "contains a Some(JourneyStatus) when a status is returned from the database" in {
        mockGetJourneyStatus(ctx, Right(List(JourneyNameAndStatus(PaymentsIntoPensions, Completed))))

        val response = service.getJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions))

        await(response) shouldBe Right(Some(Completed))
      }
    }

    "return an error result when connector returns an error" in {
      mockGetJourneyStatus(ctx, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val response = service.getJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions))

      await(response) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
    }
  }

  "saveJourneyStatus" should {

    "return a Right Unit when a journey's status is changed successfully" in {
      val ctx = JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions)
      mockSaveJourneyStatus(ctx, Completed, Right(()))

      val response = service.saveJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions), Completed)

      await(response) shouldBe Right(())
    }

    "return an error" when {
      "the connector returns an Internal Server error" in {
        val ctx = JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions)
        mockSaveJourneyStatus(
          ctx,
          Completed,
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel(INTERNAL_SERVER_ERROR.toString, "Internal Server Error"))))

        val response = service.saveJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions), Completed)

        await(response) shouldBe Left(
          APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel(INTERNAL_SERVER_ERROR.toString, "Internal Server Error")))
      }

      "the connector returns a Bad Request if journey status is parsed incorrectly" in {
        val ctx = JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions)
        mockSaveJourneyStatus(ctx, Completed, Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError)))

        val response = service.saveJourneyStatus(JourneyContext(currentTaxYear, Mtditid(mtditid), PaymentsIntoPensions), Completed)

        await(response) shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }
    }
  }
}
