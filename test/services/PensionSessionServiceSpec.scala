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
import common.TaxYear
import config._
import models.IncomeTaxUserData
import models.mongo._
import models.pension.AllPensionsData.generateSessionModelFromPrior
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class PensionSessionServiceSpec extends UnitTest with MockPensionUserDataRepository with MockIncomeTaxUserDataConnector with ScalaFutures {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate   = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate                       = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi                             = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(
    mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: PensionSessionService =
    new PensionSessionService(mockPensionUserDataRepository, mockUserDataConnector, errorHandler)(mockExecutionContext)

  "loadDataAndHandle" should {
    "invoke block if no errors retrieving session and prior data" in {
      mockFind(taxYear, user, Right(None))
      mockFindNoContent(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)(block = (_, _) => Future(Ok))

      status(response) shouldBe OK
    }
    "return a 500 if an error loading prior data" in {
      mockFind(taxYear, user, Right(None))
      mockFindFail(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return a 500 if failed to load session data" in {
      mockFind(taxYear, user, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = service.loadDataAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }
  "loadPriorAndSession" should {
    "load both prior and session data when retrieval cals are successful" in {
      val prior   = IncomeTaxUserData(pensions = anAllPensionsData.some)
      val session = aPensionsUserData

      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, prior)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, session.some.asRight)

      val result = service.loadPriorAndSession(user, TaxYear(taxYear)).value.futureValue

      result shouldBe (prior, session).asRight
    }

    "return a MongoError when there's an error interacting with the database" in {
      val mongoError = MongoError("some error")

      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, IncomeTaxUserData(None))
      super[MockPensionUserDataRepository].mockFind(taxYear, user, mongoError.asLeft)

      val result = service.loadPriorAndSession(user, TaxYear(taxYear)).value.futureValue

      result shouldBe mongoError.asLeft
    }

    "return SessionNotFound when no user session is found in the database" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, IncomeTaxUserData(None))
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(None))

      val result = service.loadPriorAndSession(user, TaxYear(taxYear)).value.futureValue

      result shouldBe SessionNotFound.asLeft
    }

    "return an APIErrorModel when there's an error loading prior data from downstream" in {
      super[MockIncomeTaxUserDataConnector].mockFindFail(nino, taxYear)

      val result = service.loadPriorAndSession(user, TaxYear(taxYear)).value.futureValue

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
      response shouldBe aPensionsCYAGeneratedFromPriorEmpty
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
      mockCreateOrUpdate(Right(()))

      val response = service.mergePriorDataToSession(taxYear, user, (_, _, _) => Ok)

      assert(status(response) === OK)
    }

    "return a successful if not update required (session == prior)" in {
      val sessionWithEmptyModel = aPensionsUserData.copy(pensions = PensionsCYAModel.emptyModels)
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(Some(sessionWithEmptyModel)))
      mockCreateOrUpdate(Right(()))

      val response = service.mergePriorDataToSession(taxYear, user, (_, _, _) => Ok)

      assert(status(response) === OK)
    }

    "return an internal server error if data not found" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Left(DataNotFound))

      val response = service.mergePriorDataToSession(taxYear, user, (_, _, _) => Ok)

      assert(status(response) === INTERNAL_SERVER_ERROR)
    }

    "return an internal server error if data not updated" in {
      super[MockIncomeTaxUserDataConnector].mockFind(nino, taxYear, noPriorData)
      super[MockPensionUserDataRepository].mockFind(taxYear, user, Right(None))
      mockCreateOrUpdate(Left(DataNotUpdated))

      val response = service.mergePriorDataToSession(taxYear, user, (_, _, _) => Ok)

      assert(status(response) === INTERNAL_SERVER_ERROR)
    }

  }
}
