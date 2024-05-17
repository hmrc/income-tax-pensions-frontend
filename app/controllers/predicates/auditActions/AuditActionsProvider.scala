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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.{ActionsProvider, AuthorisedAction}
import controllers.predicates.auditActions.PensionsAuditAction._
import models.pension.Journey
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import play.api.i18n.MessagesApi
import play.api.mvc.{ActionBuilder, AnyContent}
import services.{AuditService, PensionSessionService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

// TODO Previous test for this class was unmanageable, so it was removed. Add a new one once the new prior data are finished
@Singleton
class AuditActionsProvider @Inject() (authAction: AuthorisedAction,
                                      pensionSessionService: PensionSessionService,
                                      errorHandler: ErrorHandler,
                                      appConfig: AppConfig,
                                      auditService: AuditService)(implicit ec: ExecutionContext, messagesApi: MessagesApi)
    extends ActionsProvider(authAction, pensionSessionService, errorHandler, appConfig) {

  def paymentsIntoPensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(TaxYear(taxYear), Journey.PaymentsIntoPensions)
      .andThen(PaymentsIntoPensionsViewAuditAction(auditService))

  def paymentsIntoPensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(PaymentsIntoPensionsUpdateAuditAction(auditService))

  def unauthorisedPaymentsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(TaxYear(taxYear), Journey.UnauthorisedPayments)
      .andThen(UnauthorisedPaymentsViewAuditAction(auditService))

  def unauthorisedPaymentsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(UnauthorisedPaymentsUpdateAuditAction(auditService))

  def incomeFromOverseasPensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(IncomeFromOverseasPensionsViewAuditAction(auditService))

  def incomeFromOverseasPensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(IncomeFromOverseasPensionsUpdateAuditAction(auditService))

  def paymentsIntoOverseasPensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(PaymentsIntoOverseasPensionsViewAuditAction(auditService))

  def paymentsIntoOverseasPensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(PaymentsIntoOverseasPensionsUpdateAuditAction(auditService))

  def shortServiceRefundsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(ShortServiceRefundsViewAuditAction(auditService))

  def shortServiceRefundsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(ShortServiceRefundsUpdateAuditAction(auditService))

  def incomeFromStatePensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(IncomeFromStatePensionsViewAuditAction(auditService))

  def incomeFromStatePensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(IncomeFromStatePensionsUpdateAuditAction(auditService))

  def ukPensionIncomeViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSession(taxYear)
      .andThen(UkPensionIncomeViewAuditAction(auditService))

  def ukPensionIncomeUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(UkPensionIncomeUpdateAuditAction(auditService))

  def annualAllowancesViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(TaxYear(taxYear), Journey.AnnualAllowances)
      .andThen(AnnualAllowancesViewAuditAction(auditService))

  def annualAllowancesUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(AnnualAllowancesUpdateAuditAction(auditService))

  def transfersIntoOverseasPensionsViewAuditing(taxYear: Int): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(TaxYear(taxYear), Journey.AnnualAllowances)
      .andThen(TransfersIntoOverseasPensionsViewAuditAction(auditService))

  def transfersIntoOverseasPensionsUpdateAuditing(taxYear: Int): ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] =
    authoriseWithSessionAndPrior(taxYear)
      .andThen(TransfersIntoOverseasPensionsUpdateAuditAction(auditService))
}
