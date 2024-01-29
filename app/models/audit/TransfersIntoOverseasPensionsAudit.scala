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
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import play.api.libs.json.{Json, OWrites}

case class TransfersIntoOverseasPensionsAudit(taxYear: Int,
                                              userType: String,
                                              nino: String,
                                              mtdItId: String,
                                              transfersIntoOverseasPensions: TransfersIntoOverseasPensionsViewModel,
                                              priorTransfersIntoOverseasPensions: Option[TransfersIntoOverseasPensionsViewModel] = None) {

  private val amend  = "AmendTransfersIntoOverseasPensions"
  private val create = "CreateTransfersIntoOverseasPensions"
  private val view   = "ViewTransfersIntoOverseasPensions"

  def toAuditModelAmend: AuditModel[TransfersIntoOverseasPensionsAudit] = toAuditModel(amend)

  def toAuditModelCreate: AuditModel[TransfersIntoOverseasPensionsAudit] = toAuditModel(create)

  def toAuditModelView: AuditModel[TransfersIntoOverseasPensionsAudit] = toAuditModel(view)

  private def toAuditModel(name: String): AuditModel[TransfersIntoOverseasPensionsAudit] = AuditModel(name, name, this)
}

object TransfersIntoOverseasPensionsAudit {

  def apply(taxYear: Int,
            user: User,
            transfersIntoOverseasPensions: TransfersIntoOverseasPensionsViewModel,
            priorTransfersIntoOverseasPensions: Option[TransfersIntoOverseasPensionsViewModel]): TransfersIntoOverseasPensionsAudit =
    TransfersIntoOverseasPensionsAudit(
      taxYear,
      user.affinityGroup,
      user.nino,
      user.mtditid,
      transfersIntoOverseasPensions,
      priorTransfersIntoOverseasPensions)

  implicit val writes: OWrites[TransfersIntoOverseasPensionsAudit] = Json.writes[TransfersIntoOverseasPensionsAudit]

  def amendAudit(user: User, sessionData: PensionsUserData, priorData: Option[AllPensionsData]): TransfersIntoOverseasPensionsAudit =
    TransfersIntoOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      transfersIntoOverseasPensions = sessionData.pensions.transfersIntoOverseasPensions,
      priorTransfersIntoOverseasPensions = priorData.map(pd => AllPensionsData.generateCyaFromPrior(pd).transfersIntoOverseasPensions)
    )

  def standardAudit(user: User, sessionData: PensionsUserData): TransfersIntoOverseasPensionsAudit =
    TransfersIntoOverseasPensionsAudit(
      taxYear = sessionData.taxYear,
      userType = user.affinityGroup,
      nino = sessionData.nino,
      mtdItId = sessionData.mtdItId,
      transfersIntoOverseasPensions = sessionData.pensions.transfersIntoOverseasPensions
    )

}
