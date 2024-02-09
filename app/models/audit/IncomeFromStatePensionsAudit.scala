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
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.libs.json.{Json, OWrites}

case class IncomeFromStatePensionsAudit(taxYear: Int,
                                        userType: String,
                                        nino: String,
                                        mtdItId: String,
                                        incomeFromStatePensions: IncomeFromPensionsViewModel,
                                        priorIncomeFromStatePensions: Option[IncomeFromPensionsViewModel] = None) {

  private val amend  = "AmendIncomeFromStatePensions"
  private val create = "CreateIncomeFromStatePensions"
  private val view   = "ViewIncomeFromStatePensions"

  def toAuditModelAmend: AuditModel[IncomeFromStatePensionsAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[IncomeFromStatePensionsAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[IncomeFromStatePensionsAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[IncomeFromStatePensionsAudit] = AuditModel(name, name, this)
}

object IncomeFromStatePensionsAudit {

  def apply(taxYear: Int,
            user: User,
            incomeFromStatePensions: IncomeFromPensionsViewModel,
            priorIncomeFromStatePensions: Option[IncomeFromPensionsViewModel]): IncomeFromStatePensionsAudit =
    IncomeFromStatePensionsAudit(
      taxYear,
      user.affinityGroup,
      user.nino,
      user.mtditid,
      incomeFromStatePensions,
      priorIncomeFromStatePensions
    )

  implicit val writes: OWrites[IncomeFromStatePensionsAudit] = Json.writes[IncomeFromStatePensionsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): IncomeFromStatePensionsAudit =
    IncomeFromStatePensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      incomeFromStatePensions = sessionData.pensions.incomeFromPensions,
      priorIncomeFromStatePensions = priorData.map(pd => AllPensionsData.generateSessionModelFromPrior(pd).incomeFromPensions)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): IncomeFromStatePensionsAudit =
    IncomeFromStatePensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      incomeFromStatePensions = sessionData.pensions.incomeFromPensions
    )

}
