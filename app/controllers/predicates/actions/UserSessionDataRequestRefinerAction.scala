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

import config.ErrorHandler
import models.AuthorisationRequest
import models.requests.UserSessionDataRequest
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{ActionRefiner, Result}
import services.{PensionSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

case class UserSessionDataRequestRefinerAction(taxYear: Int, pensionSessionService: PensionSessionService, errorHandler: ErrorHandler)(implicit
    ec: ExecutionContext)
    extends ActionRefiner[AuthorisationRequest, UserSessionDataRequest]
    with FrontendHeaderCarrierProvider {

  override protected[predicates] def executionContext: ExecutionContext = ec

  override protected[predicates] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] =
    pensionSessionService.loadSessionData(taxYear, input.user).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR)(input.request))
      case Right(optPensionsUserData) =>
        RedirectService.redirectBasedOnRequest(optPensionsUserData, taxYear) match {
          case Left(value) => Left(value)
          case Right(pensionsUserData) =>
            Right(UserSessionDataRequest(pensionsUserData, input.user, input.request))
        }
    }
}
