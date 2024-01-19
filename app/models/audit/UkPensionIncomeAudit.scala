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
import models.audit.UkPensionIncomeAudit.AuditUkPensionIncome
import models.mongo.PensionsUserData
import models.pension.AllPensionsData
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.libs.json.{Json, OWrites}

case class UkPensionIncomeAudit(taxYear: Int,
                                userType: String,
                                nino: String,
                                mtdItId: String,
                                ukPensionIncome: AuditUkPensionIncome,
                                priorUkPensionIncome: Option[AuditUkPensionIncome] = None) {

    private val amend = "AmendUkPensionIncome"
    private val create = "CreateUkPensionIncome"
    private val view = "ViewUkPensionIncome"

    def toAuditModelAmend: AuditModel[UkPensionIncomeAudit] = toAuditModel(amend)

    def toAuditModelCreate: AuditModel[UkPensionIncomeAudit] = toAuditModel(create)

    def toAuditModelView: AuditModel[UkPensionIncomeAudit] = toAuditModel(view)

    private def toAuditModel(name: String): AuditModel[UkPensionIncomeAudit] = AuditModel(name, name, this)
}


object UkPensionIncomeAudit {
  
  case class AuditUkPensionIncome(uKPensionIncomesQuestion: Option[Boolean] = None,
                                  uKPensionIncomes: Seq[UkPensionIncomeViewModel] = Seq.empty)
  
  object AuditUkPensionIncome {
    implicit val auditUkPensionIncomeWrites: OWrites[AuditUkPensionIncome] = Json.writes[AuditUkPensionIncome]
  }

  def apply(taxYear: Int,
            user: User,
            ukPensionIncome: AuditUkPensionIncome,
            priorUkPensionIncome: Option[AuditUkPensionIncome]): UkPensionIncomeAudit = {

    UkPensionIncomeAudit(
      taxYear, user.affinityGroup, user.nino, user.mtditid, ukPensionIncome, priorUkPensionIncome
    )
  }

  implicit val writes: OWrites[UkPensionIncomeAudit] = Json.writes[UkPensionIncomeAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): UkPensionIncomeAudit =
    UkPensionIncomeAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      ukPensionIncome = AuditUkPensionIncome(
        sessionData.pensions.incomeFromPensions.uKPensionIncomesQuestion,
        sessionData.pensions.incomeFromPensions.uKPensionIncomes),
      priorUkPensionIncome =  priorData.map(pd => AllPensionsData.generateUkPensionCyaFromPrior(pd))
          .map({case (ukPensionIncomeQ, ukPensionIncomes) => AuditUkPensionIncome(ukPensionIncomeQ, ukPensionIncomes)})
    )

  def standardAudit(user: User, sessionData: PensionsUserData): UkPensionIncomeAudit =
    UkPensionIncomeAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      ukPensionIncome = AuditUkPensionIncome(
        sessionData.pensions.incomeFromPensions.uKPensionIncomesQuestion,
        sessionData.pensions.incomeFromPensions.uKPensionIncomes)
    )
}
