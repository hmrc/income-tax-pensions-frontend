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
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.libs.json.{Json, OWrites}

case class UnauthorisedPaymentsAudit(taxYear: Int,
                                     userType: String,
                                     nino: String,
                                     mtdItId: String,
                                     unauthorisedPayments: UnauthorisedPaymentsViewModel,
                                     priorUnauthorisedPayments: Option[UnauthorisedPaymentsViewModel] = None) {

  private val amend = "AmendUnauthorisedPayments"
  private val create = "CreateUnauthorisedPayments"
  private val view = "ViewUnauthorisedPayments"

  def toAuditModelAmend: AuditModel[UnauthorisedPaymentsAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[UnauthorisedPaymentsAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[UnauthorisedPaymentsAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[UnauthorisedPaymentsAudit] = AuditModel(name, name, this)
}

object UnauthorisedPaymentsAudit {

  def apply(taxYear: Int,
            user: User,
            unauthorisedPayments: UnauthorisedPaymentsViewModel,
            priorUnauthorisedPayments: Option[UnauthorisedPaymentsViewModel]): UnauthorisedPaymentsAudit = {

    UnauthorisedPaymentsAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, unauthorisedPayments, priorUnauthorisedPayments
    )
  }

  implicit val writes: OWrites[UnauthorisedPaymentsAudit] = Json.writes[UnauthorisedPaymentsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): UnauthorisedPaymentsAudit =
    UnauthorisedPaymentsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      unauthorisedPayments = sessionData.pensions.unauthorisedPayments,
      priorUnauthorisedPayments = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).unauthorisedPayments)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): UnauthorisedPaymentsAudit =
    UnauthorisedPaymentsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      unauthorisedPayments = sessionData.pensions.unauthorisedPayments
    )
}
