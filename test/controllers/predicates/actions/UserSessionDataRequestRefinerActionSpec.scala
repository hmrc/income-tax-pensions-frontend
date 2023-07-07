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

package controllers.predicates.actions

import builders.AuthorisationRequestBuilder.anAuthorisationRequest
import builders.PensionsUserDataBuilder.{aPensionsUserData, taxYear}
import models.requests.UserSessionDataRequest
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.InternalServerError
import support.UnitTest
import support.mocks.{MockErrorHandler, MockPensionSessionService}

import scala.concurrent.ExecutionContext

class UserSessionDataRequestRefinerActionSpec extends UnitTest
  with MockPensionSessionService
  with MockErrorHandler {
  
  private val executionContext = ExecutionContext.global
 

  private val underTest = UserSessionDataRequestRefinerAction(taxYear, mockPensionSessionService, mockErrorHandler)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "handle InternalServerError when when getting session data result in an error" in {
      mockGetPensionSessionData(taxYear,
        Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues."))))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(InternalServerError)
    }

    "return StateBenefitsUserData when the service returns data" in {
      mockGetPensionSessionData(taxYear, Right(Some(aPensionsUserData)))

      await(underTest.refine(anAuthorisationRequest)) shouldBe
        Right(UserSessionDataRequest(aPensionsUserData, anAuthorisationRequest.user, anAuthorisationRequest.request))
    }
  }
}
