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
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.libs.json.{Json, OWrites}

case class AnnualAllowancesAudit(taxYear: Int,
                                 userType: String,
                                 nino: String,
                                 mtdItId: String,
                                 annualAllowances: PensionAnnualAllowancesViewModel,
                                 priorAnnualAllowances: Option[PensionAnnualAllowancesViewModel] = None) {

  private val amend = "AmendAnnualAllowances"
  private val create = "CreateAnnualAllowances"
  private val view = "ViewAnnualAllowances"

  def toAuditModelAmend: AuditModel[AnnualAllowancesAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[AnnualAllowancesAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[AnnualAllowancesAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[AnnualAllowancesAudit] = AuditModel(name, name, this)
}

object AnnualAllowancesAudit {

  def apply(taxYear: Int,
            user: User,
            annualAllowances: PensionAnnualAllowancesViewModel,
            priorAnnualAllowances: Option[PensionAnnualAllowancesViewModel]): AnnualAllowancesAudit = {

    AnnualAllowancesAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, annualAllowances, priorAnnualAllowances
    )
  }

  implicit val writes: OWrites[AnnualAllowancesAudit] = Json.writes[AnnualAllowancesAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): AnnualAllowancesAudit =
    AnnualAllowancesAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      annualAllowances = sessionData.pensions.pensionsAnnualAllowances,
      priorAnnualAllowances = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).pensionsAnnualAllowances)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): AnnualAllowancesAudit =
    AnnualAllowancesAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      annualAllowances = sessionData.pensions.pensionsAnnualAllowances
    )
}
