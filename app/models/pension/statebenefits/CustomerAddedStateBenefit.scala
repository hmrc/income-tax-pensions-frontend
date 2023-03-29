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

package models.pension.statebenefits

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

import java.time.{Instant, LocalDate}
import java.util.UUID

case class CustomerAddedStateBenefit(benefitId: UUID,
                                     startDate: LocalDate,
                                     endDate: Option[LocalDate] = None,
                                     submittedOn: Option[Instant] = None,
                                     amount: Option[BigDecimal] = None,
                                     taxPaid: Option[BigDecimal] = None)

object CustomerAddedStateBenefit {
  implicit val format: OFormat[CustomerAddedStateBenefit] = Json.format[CustomerAddedStateBenefit]
}

case class EncryptedCustomerAddedStateBenefit(benefitId: EncryptedValue,
                                              startDate: EncryptedValue,
                                              endDate: Option[EncryptedValue],
                                              submittedOn: Option[EncryptedValue],
                                              amount: Option[EncryptedValue],
                                              taxPaid: Option[EncryptedValue]
                                             )

object EncryptedCustomerAddedStateBenefit {
  implicit val format: OFormat[EncryptedCustomerAddedStateBenefit] = Json.format[EncryptedCustomerAddedStateBenefit]
}
