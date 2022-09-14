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

package models.pension.charges

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class PensionAnnualAllowancesViewModel(
                                            reducedAnnualAllowanceQuestion: Option[Boolean] = None,
                                            moneyPurchaseAnnualAllowance: Option[Boolean] = None,
                                            taperedAnnualAllowance: Option[Boolean] = None,
                                            aboveAnnualAllowanceQuestion: Option[Boolean] = None,
                                            aboveAnnualAllowance: Option[BigDecimal] = None,
                                            pensionProvidePaidAnnualAllowanceQuestion: Option[Boolean] = None,
                                            taxPaidByPensionProvider: Option[BigDecimal] = None,
                                            pensionSchemeTaxReference: Option[Seq[String]] = None)

object PensionAnnualAllowancesViewModel {
  implicit val format: OFormat[PensionAnnualAllowancesViewModel] = Json.format[PensionAnnualAllowancesViewModel]
}

case class EncryptedPensionAnnualAllowancesViewModel(
                                                     reducedAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     moneyPurchaseAnnualAllowance: Option[EncryptedValue] = None,
                                                     taperedAnnualAllowance: Option[EncryptedValue] = None,
                                                     aboveAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     aboveAnnualAllowance: Option[EncryptedValue] = None,
                                                     pensionProvidePaidAnnualAllowanceQuestion: Option[EncryptedValue] = None,
                                                     taxPaidByPensionProvider: Option[EncryptedValue] = None,
                                                     pensionSchemeTaxReference: Option[Seq[EncryptedValue]] = None)

object EncryptedPensionAnnualAllowancesViewModel {
  implicit val format: OFormat[EncryptedPensionAnnualAllowancesViewModel] = Json.format[EncryptedPensionAnnualAllowancesViewModel]
}
