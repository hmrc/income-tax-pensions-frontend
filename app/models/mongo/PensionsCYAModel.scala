/*
 * Copyright 2022 HM Revenue & Customs
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

import models.pension.charges.{EncryptedPensionAnnualAllowancesViewModel, PensionAnnualAllowancesViewModel}
import models.pension.reliefs.{EncryptedPaymentsIntoPensionViewModel, PaymentsIntoPensionViewModel}
import play.api.libs.json.{Json, OFormat}

case class PensionsCYAModel(paymentsIntoPension: PaymentsIntoPensionViewModel,
                            pensionsAnnualAllowances: PensionAnnualAllowancesViewModel)

object PensionsCYAModel {
  implicit val format: OFormat[PensionsCYAModel] = Json.format[PensionsCYAModel]
}

case class EncryptedPensionCYAModel(encryptedPaymentsIntoPension: EncryptedPaymentsIntoPensionViewModel,
                                    encryptedPensionAnnualAllowances: EncryptedPensionAnnualAllowancesViewModel)

object EncryptedPensionCYAModel {
  implicit val format: OFormat[EncryptedPensionCYAModel] = Json.format[EncryptedPensionCYAModel]
}
