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

package models.audit

import models.mongo.PensionsUserData
import models.pension.AllPensionsData
import models.User
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.libs.json.{Json, OWrites}

case class PaymentsIntoPensionsAudit(
  taxYear: Int,
  userType: String,
  nino: String,
  mtdItId: String,
  paymentsIntoPension: PaymentsIntoPensionsViewModel,
  priorPaymentsIntoPension: Option[PaymentsIntoPensionsViewModel] = None
) {
  private val amend = "AmendPaymentsIntoPension"
  private val create = "CreatePaymentsIntoPension"
  private val view = "ViewPaymentsIntoPension"
  
  def toAuditModelAmend: AuditModel[PaymentsIntoPensionsAudit] = toAuditModel(amend)
  
  def toAuditModelCreate: AuditModel[PaymentsIntoPensionsAudit] = toAuditModel(create)
  
  def toAuditModelView: AuditModel[PaymentsIntoPensionsAudit] = toAuditModel(view)
  
  private def toAuditModel(name: String): AuditModel[PaymentsIntoPensionsAudit] = AuditModel(name, name, this)
}

object PaymentsIntoPensionsAudit {

  def apply(
    taxYear: Int,
    user: User,
    paymentsIntoPension: PaymentsIntoPensionsViewModel,
    priorPaymentsIntoPension: Option[PaymentsIntoPensionsViewModel]
  ): PaymentsIntoPensionsAudit = {
    PaymentsIntoPensionsAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, paymentsIntoPension, priorPaymentsIntoPension
    )
  }

  implicit val writes: OWrites[PaymentsIntoPensionsAudit] = Json.writes[PaymentsIntoPensionsAudit]
  
  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]) : PaymentsIntoPensionsAudit =
    PaymentsIntoPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      paymentsIntoPension = sessionData.pensions.paymentsIntoPension,
      priorPaymentsIntoPension = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).paymentsIntoPension)
    )
  
  def standardAudit(user: User, sessionData: PensionsUserData): PaymentsIntoPensionsAudit =
    PaymentsIntoPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      paymentsIntoPension = sessionData.pensions.paymentsIntoPension
    )
}
