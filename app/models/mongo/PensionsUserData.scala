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

package models.mongo

import common.TaxYear
import models.User
import models.pension.Journey
import models.pension.Journey.{AnnualAllowances, PaymentsIntoPensions, StatePension, UkPensionIncome, UnauthorisedPayments}
import models.pension.charges.{PensionAnnualAllowancesViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, ZoneOffset, ZonedDateTime}

case class PensionsUserData(sessionId: String,
                            mtdItId: String,
                            nino: String,
                            taxYear: Int,
                            isPriorSubmission: Boolean, // TODO Where do we use that? Prior submission for which journey?
                            pensions: PensionsCYAModel,
                            lastUpdated: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)) {
  private def now = ZonedDateTime.now(ZoneOffset.UTC)

  def removeJourneyAnswers(journey: Journey): PensionsUserData =
    journey match {
      case PaymentsIntoPensions =>
        copy(lastUpdated = now, pensions = pensions.copy(paymentsIntoPension = PaymentsIntoPensionsViewModel.empty))
      case UkPensionIncome =>
        copy(lastUpdated = now, pensions = pensions.copy(incomeFromPensions = pensions.incomeFromPensions.removeUkPensionIncome))
      case StatePension =>
        copy(lastUpdated = now, pensions = pensions.copy(incomeFromPensions = pensions.incomeFromPensions.removeStatePension))
      case AnnualAllowances =>
        copy(lastUpdated = now, pensions = pensions.copy(pensionsAnnualAllowances = PensionAnnualAllowancesViewModel.empty))
      case UnauthorisedPayments =>
        copy(lastUpdated = now, pensions = pensions.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel.empty))
      case _ => ??? // TODO will be done when other journeys are implemented
    }
}

object PensionsUserData extends MongoJavatimeFormats {
  type SessionData = PensionsUserData

  implicit val formats: Format[PensionsUserData] = Json.format[PensionsUserData]

  def empty(user: User, taxYear: TaxYear): PensionsUserData =
    PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear.endYear,
      isPriorSubmission = false,
      PensionsCYAModel.emptyModels,
      Clock.systemUTC().instant().atZone(ZoneOffset.UTC)
    )

}

case class EncryptedPensionsUserData(sessionId: String,
                                     mtdItId: String,
                                     nino: String,
                                     taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     pensions: EncryptedPensionCYAModel,
                                     lastUpdated: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC))

object EncryptedPensionsUserData extends MongoJavatimeFormats {

  implicit val formats: Format[EncryptedPensionsUserData] = Json.format[EncryptedPensionsUserData]
}
