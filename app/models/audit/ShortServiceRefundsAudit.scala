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
import models.pension.charges.ShortServiceRefundsViewModel
import play.api.libs.json.{Json, OWrites}

case class ShortServiceRefundsAudit(taxYear: Int,
                                    userType: String,
                                    nino: String,
                                    mtdItId: String,
                                    shortServiceRefunds: ShortServiceRefundsViewModel,
                                    priorShortServiceRefunds: Option[ShortServiceRefundsViewModel] = None) {

  private val amend = "AmendShortServiceRefunds"
  private val create = "CreateShortServiceRefunds"
  private val view = "ViewShortServiceRefunds"

  def toAuditModelAmend: AuditModel[ShortServiceRefundsAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[ShortServiceRefundsAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[ShortServiceRefundsAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[ShortServiceRefundsAudit] = AuditModel(name, name, this)
}

object ShortServiceRefundsAudit {

  def apply(taxYear: Int,
            user: User,
            shortServiceRefunds: ShortServiceRefundsViewModel,
            priorShortServiceRefunds: Option[ShortServiceRefundsViewModel]): ShortServiceRefundsAudit = {

    ShortServiceRefundsAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, shortServiceRefunds, priorShortServiceRefunds
    )
  }

  implicit val writes: OWrites[ShortServiceRefundsAudit] = Json.writes[ShortServiceRefundsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): ShortServiceRefundsAudit =
    ShortServiceRefundsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      shortServiceRefunds = sessionData.pensions.shortServiceRefunds,
      priorShortServiceRefunds = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).shortServiceRefunds)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): ShortServiceRefundsAudit =
    ShortServiceRefundsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      shortServiceRefunds = sessionData.pensions.shortServiceRefunds
    )
}
