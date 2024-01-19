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

package models.pension.statebenefits

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

import java.time.{Instant, LocalDate}
import java.util.UUID

case class StateBenefit(benefitId: UUID,
                        startDate: LocalDate,
                        endDate: Option[LocalDate] = None,
                        dateIgnored: Option[Instant] = None,
                        submittedOn: Option[Instant] = None,
                        amount: Option[BigDecimal] = None,
                        taxPaid: Option[BigDecimal] = None)

object StateBenefit {
  implicit val format: OFormat[StateBenefit] = Json.format[StateBenefit]
}

case class EncryptedStateBenefit(benefitId: EncryptedValue,
                                 startDate: EncryptedValue,
                                 dateIgnored: Option[EncryptedValue],
                                 submittedOn: Option[EncryptedValue],
                                 endDate: Option[EncryptedValue],
                                 amount: Option[EncryptedValue],
                                 taxPaid: Option[EncryptedValue]
                                )

object EncryptedStateBenefit {
  implicit val format: OFormat[EncryptedStateBenefit] = Json.format[EncryptedStateBenefit]
}

