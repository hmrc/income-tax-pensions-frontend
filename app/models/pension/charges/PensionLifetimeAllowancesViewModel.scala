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

import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.booleanDecryptor
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.booleanEncryptor
import utils.{EncryptedValue, SecureGCMCipher}

case class PensionLifetimeAllowancesViewModel(
                                            aboveLifetimeAllowanceQuestion: Option[Boolean] = None,
                                            pensionAsLumpSumQuestion: Option[Boolean] = None,
                                            pensionAsLumpSum: Option[LifetimeAllowance] = None,
                                            pensionPaidAnotherWayQuestion: Option[Boolean] = None,
                                            pensionPaidAnotherWay: Option[LifetimeAllowance] = None) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedPensionLifetimeAllowancesViewModel =
    EncryptedPensionLifetimeAllowancesViewModel(
      aboveLifetimeAllowanceQuestion = aboveLifetimeAllowanceQuestion.map(_.encrypted),
      pensionAsLumpSumQuestion = pensionAsLumpSumQuestion.map(_.encrypted),
      pensionAsLumpSum = pensionAsLumpSum.map(_.encrypted),
      pensionPaidAnotherWayQuestion = pensionPaidAnotherWayQuestion.map(_.encrypted),
      pensionPaidAnotherWay = pensionPaidAnotherWay.map(_.encrypted)

    )
}

object PensionLifetimeAllowancesViewModel {
  implicit val format: OFormat[PensionLifetimeAllowancesViewModel] = Json.format[PensionLifetimeAllowancesViewModel]
}

case class EncryptedPensionLifetimeAllowancesViewModel(
                                                     aboveLifetimeAllowanceQuestion: Option[EncryptedValue] = None,
                                                     pensionAsLumpSumQuestion: Option[EncryptedValue] = None,
                                                     pensionAsLumpSum: Option[EncryptedLifetimeAllowance] = None,
                                                     pensionPaidAnotherWayQuestion: Option[EncryptedValue] = None,
                                                     pensionPaidAnotherWay: Option[EncryptedLifetimeAllowance] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): PensionLifetimeAllowancesViewModel =
    PensionLifetimeAllowancesViewModel(
      aboveLifetimeAllowanceQuestion = aboveLifetimeAllowanceQuestion.map(_.decrypted[Boolean]),
      pensionAsLumpSumQuestion = pensionAsLumpSumQuestion.map(_.decrypted[Boolean]),
      pensionAsLumpSum = pensionAsLumpSum.map(_.decrypted),
      pensionPaidAnotherWayQuestion = pensionPaidAnotherWayQuestion.map(_.decrypted[Boolean]),
      pensionPaidAnotherWay = pensionPaidAnotherWay.map(_.decrypted)
    )
}

object EncryptedPensionLifetimeAllowancesViewModel {
  implicit val format: OFormat[EncryptedPensionLifetimeAllowancesViewModel] = Json.format[EncryptedPensionLifetimeAllowancesViewModel]
}
