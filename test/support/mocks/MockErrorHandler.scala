/*
 * Copyright 2025 HM Revenue & Customs
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

package support.mocks

import config.ErrorHandler
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Request, Result}

import scala.concurrent.Future

trait MockErrorHandler extends MockitoSugar  {

  val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

  def mockHandleError(status: Int, result: Result): ScalaOngoingStubbing[Result] = {
    when(mockErrorHandler.handleError(eqTo(status))(any[Request[_]]))
      .thenReturn(result)

  }

  def mockInternalServerError: ScalaOngoingStubbing[Result] = {
    when(mockErrorHandler.internalServerError()(any[Request[_]]))
      .thenReturn(InternalServerError)

  }

  def mockFutureInternalServerError(errString: String): ScalaOngoingStubbing[Future[Result]] = {
    when(mockErrorHandler.futureInternalServerError()(any[Request[_]]))
      .thenReturn(Future.successful(InternalServerError(errString)))

    //    (mockErrorHandler
    //      .futureInternalServerError()(_: Request[_]))
    //      .expects(*)
    //      .returns(Future.successful(InternalServerError(errString)))
  }
}
