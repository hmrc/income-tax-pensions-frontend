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

import builders.AllPensionsDataBuilder.anAllPensionDataEmpty
import builders.PensionsCYAModelBuilder._
import config._
import models.mongo._
import models.pension.AllPensionsData.generateCyaFromPrior
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class PensionSessionServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockIncomeTaxUserDataConnector
  with ScalaFutures {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: PensionSessionService =
    new PensionSessionService(mockPensionUserDataRepository, mockUserDataConnector, mockAppConfig,
      errorHandler, mockExecutionContext)

  private val user = authorisationRequest.user
  
  //TODO add view models
  val pensionCYA: PensionsCYAModel = aPensionsCYAEmptyModel

  val pensionDataFull: PensionsUserData = PensionsUserData(
    sessionId, "1234567890", nino, taxYear, isPriorSubmission = true,
    pensionCYA, testClock.now()
  )

  "getAndHandle" should {
    "redirect if no data and redirect is set to true" in {
      mockFind(taxYear, user, Right(None))
      mockFindNoContent(nino, taxYear)
      val response = service.getAndHandle(taxYear, redirectWhenNoPrior = true, user = user)((_, _) => Future(Ok))

      status(response) shouldBe SEE_OTHER
    }

    "return an error if the call failed" in {
      mockFind(taxYear, user, Right(None))
      mockFindFail(nino, taxYear)
      val response = service.getAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, user, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)
      val response = service.getAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".createOrUpdateSessionData" should {
    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = service.createOrUpdateSessionData(user,
        pensionCYA, taxYear, isPriorSubmission = true,
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" in {
      mockCreateOrUpdate(pensionDataFull, Left(DataNotUpdated))
      val response: Future[Result] = service.createOrUpdateSessionData(user,
        pensionCYA, taxYear, isPriorSubmission = true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  "generateCyaFromPrior" should {
    "generate a PensionsCYAModel from prior AllPensionsData" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = generateCyaFromPrior(anAllPensionDataEmpty)
      response shouldBe aPensionsCYAGeneratedFromPriorEmpty
    }
  }


  ".createOrUpdateSessionData" should {
    "return Right(unit) when createOrUpdate succeeds" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = await(service.createOrUpdateSessionData(pensionDataFull))
      response shouldBe Right(())
    }

    "return Left DB Error(400) when createOrUpdate fails" in {
      mockCreateOrUpdate(pensionDataFull, Left(DataNotUpdated))
      val Left(response) = await(service.createOrUpdateSessionData(pensionDataFull))
      response shouldBe a[DatabaseError]
    }
  }
}
