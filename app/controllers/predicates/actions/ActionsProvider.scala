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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import models.AuthorisationRequest
import models.pension.Journey
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import play.api.i18n.MessagesApi
import play.api.mvc.{ActionBuilder, ActionFunction, AnyContent, Result}
import services.PensionSessionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ActionsProvider @Inject() (authAction: AuthorisedAction,
                                 pensionSessionService: PensionSessionService,
                                 errorHandler: ErrorHandler,
                                 appConfig: AppConfig)(implicit ec: ExecutionContext, val messagesApi: MessagesApi) {

  // TODO: Decide whether we need TaxYearAction or not.
  def endOfYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction(taxYear)(appConfig, messagesApi, ec))

  def authoriseWithSession(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    endOfYear(taxYear)
      .andThen(UserRequestWithSessionRefinerAction(taxYear, pensionSessionService, errorHandler))

  def authoriseWithSessionAndPrior(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(UserPriorAndSessionDataRequestRefinerAction(taxYear, pensionSessionService, errorHandler))

  def authoriseWithSessionAndPrior(taxYear: TaxYear, journey: Journey): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear.endYear)
      .andThen(LoadPriorDataToSessionAction(taxYear, journey, pensionSessionService, errorHandler))

  def authoriseWithSessionAndPriorAuthRequest(taxYear: TaxYear, journey: Journey): ActionBuilder[AuthorisationRequest, AnyContent] = {
    val toAuthRequestAction = new ActionFunction[UserSessionDataRequest, AuthorisationRequest] {
      def invokeBlock[A](request: UserSessionDataRequest[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] =
        block(request.toAuthorisationRequest)

      protected def executionContext: ExecutionContext = ec
    }

    authoriseWithSession(taxYear.endYear)
      .andThen(LoadPriorDataToSessionAction(taxYear, journey, pensionSessionService, errorHandler))
      .andThen(toAuthRequestAction)
  }

}
