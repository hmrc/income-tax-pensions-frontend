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

import models.User
import models.mongo.PensionsUserData
import models.pension.AllPensionsData
import models.pension.charges.PensionLifetimeAllowancesViewModel
import play.api.libs.json.{Json, OWrites}

case class LifetimeAllowancesAudit(taxYear: Int,
                           userType: String,
                           nino: String, 
                           mtdItId: String,
                           lifetimeAllowances: PensionLifetimeAllowancesViewModel,
                           priorLifetimeAllowances: Option[PensionLifetimeAllowancesViewModel] = None) {

    private val amend = "AmendLifetimeAllowances"
    private val create = "CreateLifetimeAllowances"
    private val view = "ViewLifetimeAllowances"

    def toAuditModelAmend: AuditModel[LifetimeAllowancesAudit] = toAuditModel(amend)

    def toAuditModelCreate: AuditModel[LifetimeAllowancesAudit] = toAuditModel(create)

    def toAuditModelView: AuditModel[LifetimeAllowancesAudit] = toAuditModel(view)

    private def toAuditModel(name: String): AuditModel[LifetimeAllowancesAudit] = AuditModel(name, name, this)
}


object LifetimeAllowancesAudit {

  def apply(taxYear: Int,
            user: User,
            lifetimeAllowances: PensionLifetimeAllowancesViewModel,
            priorLifetimeAllowances: Option[PensionLifetimeAllowancesViewModel]): LifetimeAllowancesAudit = {

    LifetimeAllowancesAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, lifetimeAllowances, priorLifetimeAllowances
    )
  }

  implicit val writes: OWrites[LifetimeAllowancesAudit] = Json.writes[LifetimeAllowancesAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): LifetimeAllowancesAudit =
    LifetimeAllowancesAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      lifetimeAllowances = sessionData.pensions.pensionLifetimeAllowances,
      priorLifetimeAllowances = priorData.map(pd => AllPensionsData.generateLifetimeAllowanceCyaFromPrior(pd))
    )

  def standardAudit(user: User, sessionData: PensionsUserData): LifetimeAllowancesAudit =
    LifetimeAllowancesAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      lifetimeAllowances = sessionData.pensions.pensionLifetimeAllowances
    )
}
