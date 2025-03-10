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

package support.mocks

import config.ErrorHandler
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Request, Result}

import scala.concurrent.Future

trait MockErrorHandler extends MockitoSugar {

  val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

  def mockHandleError(status: Int, result: Result): Unit =
    when(mockErrorHandler.handleError(eqTo(status))(any[Request[_]]))
      .thenReturn(result)

  def mockInternalServerError(): Unit =
    when(mockErrorHandler.internalServerError()(any[Request[_]]))
      .thenReturn(InternalServerError)

  def mockFutureInternalServerError(errString: String): Unit =
    when(mockErrorHandler.futureInternalServerError()(any[Request[_]]))
      .thenReturn(Future.successful(InternalServerError(errString)))
}