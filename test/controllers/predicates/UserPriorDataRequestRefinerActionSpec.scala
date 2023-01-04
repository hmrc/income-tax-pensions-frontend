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

package controllers.predicates

import builders.AuthorisationRequestBuilder.anAuthorisationRequest
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.taxYear
import models.{APIErrorBodyModel, APIErrorModel}
import models.requests.UserPriorDataRequest
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.InternalServerError
import support.UnitTest
import support.mocks.{MockErrorHandler, MockPensionSessionService}

import scala.concurrent.ExecutionContext

class UserPriorDataRequestRefinerActionSpec extends UnitTest
  with MockPensionSessionService
  with MockErrorHandler {

//  private val anyTaxYear = 2022
  private val executionContext = ExecutionContext.global

  private val underTest = UserPriorDataRequestRefinerAction(taxYear, mockPensionSessionService, mockErrorHandler)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "handle InternalServerError when when getting prior data result in an error" in {
      mockGetPriorData(taxYear, anAuthorisationRequest.user,
        Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues."))))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(InternalServerError)
    }

    "return IncomeTaxUserData when the service returns data" in {
      mockGetPriorData(taxYear, anAuthorisationRequest.user, Right(anIncomeTaxUserData))

      await(underTest.refine(anAuthorisationRequest)) shouldBe
        Right(UserPriorDataRequest(anIncomeTaxUserData, anAuthorisationRequest.user, anAuthorisationRequest.request))
    }
  }
}
