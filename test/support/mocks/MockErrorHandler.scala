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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Result

import scala.concurrent.Future

trait MockErrorHandler extends MockitoSugar  {

  val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

  def mockHandleError(status: Int, result: Result): ScalaOngoingStubbing[Result] = {

    when(mockErrorHandler.handleError(eqTo(status))(any()))
      .thenReturn(result)

    //    (mockErrorHandler
    //      .handleError(_: Int)(_: Request[_]))
    //      .expects(status, *)
    //      .returns(result)
  }

  def mockInternalServerError: ScalaOngoingStubbing[Result] = {

    when(mockErrorHandler.internalServerError()(any()))
      .thenReturn(InternalServerError)
    //    (mockErrorHandler
    //      .internalServerError()(_: Request[_]))
    //      .expects(*)
    //      .returns(InternalServerError)
  }

  def mockFutureInternalServerError(errString: String): ScalaOngoingStubbing[Future[Result]] = {

    when(mockErrorHandler.futureInternalServerError()(any()))
      .thenReturn(Future.successful(InternalServerError(errString)))

    //    (mockErrorHandler
    //      .futureInternalServerError()(_: Request[_]))
    //      .expects(*)
    //      .returns(Future.successful(InternalServerError(errString)))
  }
}
