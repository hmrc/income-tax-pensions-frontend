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

import config.{AppConfig, ErrorHandler}
import models.AuthorisationRequest
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import play.api.i18n.MessagesApi
import play.api.mvc.{ActionBuilder, AnyContent}
import services.PensionSessionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                pensionSessionService: PensionSessionService,
                                errorHandler: ErrorHandler,
                                appConfig: AppConfig)
                               (implicit ec: ExecutionContext, val messagesApi: MessagesApi) {

  def endOfYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction(taxYear)(appConfig, messagesApi))

  def userSessionDataFor(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
     endOfYear(taxYear)
      .andThen(UserSessionDataRequestRefinerAction(taxYear, pensionSessionService, errorHandler))

 def userPriorAndSessionDataFor(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
   userSessionDataFor(taxYear)
      .andThen(UserPriorAndSessionDataRequestRefinerAction(taxYear, pensionSessionService, errorHandler))
  
}

