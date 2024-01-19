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

package models.audit

import models.User
import models.mongo.PensionsUserData
import models.pension.AllPensionsData
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.libs.json.{Json, OWrites}

case class PaymentsIntoOverseasPensionsAudit(taxYear: Int,
                                             userType: String,
                                             nino: String,
                                             mtdItId: String,
                                             paymentsIntoOverseasPensions: PaymentsIntoOverseasPensionsViewModel,
                                             priorPaymentsIntoOverseasPensions: Option[PaymentsIntoOverseasPensionsViewModel] = None
                                            ) {
  private val amend = "AmendPaymentsIntoOverseasPensions"
  private val create = "CreatePaymentsIntoOverseasPensions"
  private val view = "ViewPaymentsIntoOverseasPensions"

  def toAuditModelAmend: AuditModel[PaymentsIntoOverseasPensionsAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[PaymentsIntoOverseasPensionsAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[PaymentsIntoOverseasPensionsAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[PaymentsIntoOverseasPensionsAudit] = AuditModel(name, name, this)
}

object PaymentsIntoOverseasPensionsAudit {

  def apply(taxYear: Int,
            user: User,
            paymentsIntoOverseasPensions: PaymentsIntoOverseasPensionsViewModel,
            priorPaymentsIntoOverseasPensions: Option[PaymentsIntoOverseasPensionsViewModel]
           ): PaymentsIntoOverseasPensionsAudit = {
    PaymentsIntoOverseasPensionsAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, paymentsIntoOverseasPensions, priorPaymentsIntoOverseasPensions
    )
  }

  implicit val writes: OWrites[PaymentsIntoOverseasPensionsAudit] = Json.writes[PaymentsIntoOverseasPensionsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): PaymentsIntoOverseasPensionsAudit =
    PaymentsIntoOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      paymentsIntoOverseasPensions = sessionData.pensions.paymentsIntoOverseasPensions,
      priorPaymentsIntoOverseasPensions = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).paymentsIntoOverseasPensions)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): PaymentsIntoOverseasPensionsAudit =
    PaymentsIntoOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      paymentsIntoOverseasPensions = sessionData.pensions.paymentsIntoOverseasPensions
    )
}
