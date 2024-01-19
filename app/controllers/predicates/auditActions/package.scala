/*
 * Copyright 2024 HM Revenue & Customs
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

import models.audit._
import play.api.libs.json.{Json, OWrites}

package object auditActions {

  def auditJsonPaymentsIntoPensions(auditModel: AuditModel[PaymentsIntoPensionsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[PaymentsIntoPensionsAudit]] = Json.writes[AuditModel[PaymentsIntoPensionsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonUnauthorisedPayments(auditModel: AuditModel[UnauthorisedPaymentsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[UnauthorisedPaymentsAudit]] = Json.writes[AuditModel[UnauthorisedPaymentsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonIncomeFromOverseasPensions(auditModel: AuditModel[IncomeFromOverseasPensionsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[IncomeFromOverseasPensionsAudit]] = Json.writes[AuditModel[IncomeFromOverseasPensionsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonPaymentsIntoOverseasPensions(auditModel: AuditModel[PaymentsIntoOverseasPensionsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[PaymentsIntoOverseasPensionsAudit]] = Json.writes[AuditModel[PaymentsIntoOverseasPensionsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonShortServiceRefunds(auditModel: AuditModel[ShortServiceRefundsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[ShortServiceRefundsAudit]] = Json.writes[AuditModel[ShortServiceRefundsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonIncomeFromStatePensions(auditModel: AuditModel[IncomeFromStatePensionsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[IncomeFromStatePensionsAudit]] = Json.writes[AuditModel[IncomeFromStatePensionsAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonUkPensionIncome(auditModel: AuditModel[UkPensionIncomeAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[UkPensionIncomeAudit]] = Json.writes[AuditModel[UkPensionIncomeAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonLifetimeAllowances(auditModel: AuditModel[LifetimeAllowancesAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[LifetimeAllowancesAudit]] = Json.writes[AuditModel[LifetimeAllowancesAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonAnnualAllowances(auditModel: AuditModel[AnnualAllowancesAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[AnnualAllowancesAudit]] = Json.writes[AuditModel[AnnualAllowancesAudit]]
    Json.toJson(auditModel).toString()
  }

  def auditJsonTransfersIntoOverseasPensions(auditModel: AuditModel[TransfersIntoOverseasPensionsAudit]): String = {
    implicit val audWrites: OWrites[AuditModel[TransfersIntoOverseasPensionsAudit]] = Json.writes[AuditModel[TransfersIntoOverseasPensionsAudit]]
    Json.toJson(auditModel).toString()
  }
}
