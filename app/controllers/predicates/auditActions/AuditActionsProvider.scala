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

package controllers.predicates.auditActions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.{ActionsProvider, AuthorisedAction}
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import PensionsAuditAction._
import play.api.i18n.MessagesApi
import play.api.mvc.{ActionBuilder, AnyContent}
import services.{AuditService, PensionSessionService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuditActionsProvider @Inject()(authAction: AuthorisedAction,
                                     pensionSessionService: PensionSessionService,
                                     errorHandler: ErrorHandler,
                                     appConfig: AppConfig,
                                     auditService: AuditService)
                                    (implicit ec: ExecutionContext, messagesApi: MessagesApi)
  extends ActionsProvider(authAction, pensionSessionService, errorHandler, appConfig) {
  
  def paymentsIntoPensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] = {
    userSessionDataForInYear(taxYear)
      .andThen(PaymentsIntoPensionsViewAuditAction(auditService))
  }

  def paymentsIntoPensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] = {
    userPriorAndSessionDataForInYear(taxYear)
      .andThen(PaymentsIntoPensionsUpdateAuditAction(auditService))
  }

  def unauthorisedPaymentsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] = {
    userSessionDataForInYear(taxYear)
      .andThen(UnauthorisedPaymentsViewAuditAction(auditService))
  }

  def unauthorisedPaymentsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] = {
    userPriorAndSessionDataForInYear(taxYear)
      .andThen(UnauthorisedPaymentsUpdateAuditAction(auditService))
  }
}

