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

package models.pension.charges

import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.bigDecimalDecryptor
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.bigDecimalEncryptor
import utils.{EncryptedValue, SecureGCMCipher}

case class LifetimeAllowance(amount: Option[BigDecimal] = None,
                             taxPaid: Option[BigDecimal] = None) {

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def isFinished: Boolean = amount.isDefined && taxPaid.isDefined

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedLifetimeAllowance =
    EncryptedLifetimeAllowance(
      amount = amount.map(_.encrypted),
      taxPaid = taxPaid.map(_.encrypted)
    )
}

object LifetimeAllowance {
  implicit val format: OFormat[LifetimeAllowance] = Json.format[LifetimeAllowance]
}

case class EncryptedLifetimeAllowance(amount: Option[EncryptedValue] = None,
                                      taxPaid: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): LifetimeAllowance = LifetimeAllowance(
    amount = amount.map(_.decrypted[BigDecimal]),
    taxPaid = taxPaid.map(_.decrypted[BigDecimal])
  )
}

object EncryptedLifetimeAllowance {
  implicit val format: OFormat[EncryptedLifetimeAllowance] = Json.format[EncryptedLifetimeAllowance]
}
