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
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.libs.json.{Json, OWrites}

case class IncomeFromOverseasPensionsAudit(taxYear: Int,
                                           userType: String,
                                           nino: String,
                                           mtdItId: String,
                                           incomeFromOverseasPensions: IncomeFromOverseasPensionsViewModel,
                                           priorIncomeFromOverseasPensions: Option[IncomeFromOverseasPensionsViewModel] = None) {

  private val amend  = "AmendIncomeFromOverseasPensions"
  private val create = "CreateIncomeFromOverseasPensions"
  private val view   = "ViewIncomeFromOverseasPensions"

  def toAuditModelAmend: AuditModel[IncomeFromOverseasPensionsAudit]  = toAuditModel(amend)
  def toAuditModelCreate: AuditModel[IncomeFromOverseasPensionsAudit] = toAuditModel(create)
  def toAuditModelView: AuditModel[IncomeFromOverseasPensionsAudit]   = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[IncomeFromOverseasPensionsAudit] = AuditModel(name, name, this)
}

object IncomeFromOverseasPensionsAudit {

  def apply(taxYear: Int,
            user: User,
            incomeFromOverseasPensions: IncomeFromOverseasPensionsViewModel,
            priorIncomeFromOverseasPensions: Option[IncomeFromOverseasPensionsViewModel]): IncomeFromOverseasPensionsAudit =
    IncomeFromOverseasPensionsAudit(
      taxYear,
      user.affinityGroup,
      user.nino,
      user.mtditid,
      incomeFromOverseasPensions,
      priorIncomeFromOverseasPensions
    )
  implicit val writes: OWrites[IncomeFromOverseasPensionsAudit] = Json.writes[IncomeFromOverseasPensionsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): IncomeFromOverseasPensionsAudit =
    IncomeFromOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      incomeFromOverseasPensions = sessionData.pensions.incomeFromOverseasPensions,
      priorIncomeFromOverseasPensions = priorData.map(pd => AllPensionsData.populateSessionFromPrior(pd).incomeFromOverseasPensions)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): IncomeFromOverseasPensionsAudit =
    IncomeFromOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      incomeFromOverseasPensions = sessionData.pensions.incomeFromOverseasPensions
    )

}
