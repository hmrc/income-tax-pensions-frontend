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
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class TransfersIntoOverseasPensionsViewModel(
                                                   transferPensionSavings: Option[Boolean] = None,
                                                   overseasTransferCharge: Option[Boolean] = None,
                                                   overseasTransferChargeAmount: Option[BigDecimal] = None,
                                                   pensionSchemeTransferCharge: Option[Boolean] = None,
                                                   pensionSchemeTransferChargeAmount: Option[BigDecimal] = None,
                                                   ukPensionSchemeTransfer: Seq[UkPensionScheme] = Nil) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransfersIntoOverseasPensionsViewModel =
    EncryptedTransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = transferPensionSavings.map(_.encrypted),
      overseasTransferCharge = overseasTransferCharge.map(_.encrypted),
      overseasTransferChargeAmount = overseasTransferChargeAmount.map(_.encrypted),
      pensionSchemeTransferCharge = pensionSchemeTransferCharge.map(_.encrypted),
      pensionSchemeTransferChargeAmount = pensionSchemeTransferChargeAmount.map(_.encrypted),
      ukPensionSchemeTransfer = ukPensionSchemeTransfer.map(_.encrypted())
  )
}

object TransfersIntoOverseasPensionsViewModel {
  implicit val format: OFormat[TransfersIntoOverseasPensionsViewModel] = Json.format[TransfersIntoOverseasPensionsViewModel]
}

case class EncryptedTransfersIntoOverseasPensionsViewModel(
                                                            transferPensionSavings: Option[EncryptedValue] = None,
                                                            overseasTransferCharge: Option[EncryptedValue] = None,
                                                            overseasTransferChargeAmount: Option[EncryptedValue] = None,
                                                            pensionSchemeTransferCharge: Option[EncryptedValue] = None,
                                                            pensionSchemeTransferChargeAmount: Option[EncryptedValue] = None,
                                                            ukPensionSchemeTransfer: Seq[EncryptedUkPensionScheme] = Nil) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): TransfersIntoOverseasPensionsViewModel =
    TransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = transferPensionSavings.map(_.decrypted[Boolean]),
      overseasTransferCharge = overseasTransferCharge.map(_.decrypted[Boolean]),
      overseasTransferChargeAmount = overseasTransferChargeAmount.map(_.decrypted[BigDecimal]),
      pensionSchemeTransferCharge = pensionSchemeTransferCharge.map(_.decrypted[Boolean]),
      pensionSchemeTransferChargeAmount = pensionSchemeTransferChargeAmount.map(_.decrypted[BigDecimal]),
      ukPensionSchemeTransfer = ukPensionSchemeTransfer.map(_.decrypted())
    )
}

case class UkPensionScheme(
                            ukTransferCharge: Option[Boolean],
                            name: Option[String],
                            taxReference: Option[String],
                            providerAddress: Option[String],
                            country: Option[String]){

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedUkPensionScheme = {
    EncryptedUkPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.encrypted),
      name = name.map(_.encrypted),
      taxReference = taxReference.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      country = country.map(_.encrypted)
    )
  }
}
case class EncryptedUkPensionScheme(
                                     ukTransferCharge: Option[EncryptedValue],
                                     name: Option[EncryptedValue],
                                     taxReference: Option[EncryptedValue],
                                     providerAddress: Option[EncryptedValue],
                                     country: Option[EncryptedValue]){

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): UkPensionScheme =
    UkPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      taxReference = taxReference.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      country = country.map(_.decrypted[String])
    )
}

object UkPensionScheme {
  implicit val format: OFormat[UkPensionScheme] = Json.format[UkPensionScheme]
}
