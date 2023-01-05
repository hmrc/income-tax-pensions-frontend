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
                                                   transferPensionScheme: Seq[TransferPensionScheme] = Nil) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransfersIntoOverseasPensionsViewModel =
    EncryptedTransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = transferPensionSavings.map(_.encrypted),
      overseasTransferCharge = overseasTransferCharge.map(_.encrypted),
      overseasTransferChargeAmount = overseasTransferChargeAmount.map(_.encrypted),
      pensionSchemeTransferCharge = pensionSchemeTransferCharge.map(_.encrypted),
      pensionSchemeTransferChargeAmount = pensionSchemeTransferChargeAmount.map(_.encrypted),
      transferPensionScheme = transferPensionScheme.map(_.encrypted())
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
                                                            transferPensionScheme: Seq[EncryptedTransferPensionScheme] = Nil) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): TransfersIntoOverseasPensionsViewModel =
    TransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = transferPensionSavings.map(_.decrypted[Boolean]),
      overseasTransferCharge = overseasTransferCharge.map(_.decrypted[Boolean]),
      overseasTransferChargeAmount = overseasTransferChargeAmount.map(_.decrypted[BigDecimal]),
      pensionSchemeTransferCharge = pensionSchemeTransferCharge.map(_.decrypted[Boolean]),
      pensionSchemeTransferChargeAmount = pensionSchemeTransferChargeAmount.map(_.decrypted[BigDecimal]),
      transferPensionScheme = transferPensionScheme.map(_.decrypted())
    )
}

case class TransferPensionScheme(
                                  ukTransferQuestion: Option[Boolean],
                                  name: Option[String],
                                  taxReference: Option[String],
                                  providerAddress: Option[String],
                                  countryCode: Option[String]){

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransferPensionScheme = {
    EncryptedTransferPensionScheme(
      ukTransferQuestion = ukTransferQuestion.map(_.encrypted),
      name = name.map(_.encrypted),
      taxReference = taxReference.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      countryCode = countryCode.map(_.encrypted)
    )
  }
}
case class EncryptedTransferPensionScheme(
                                     ukTransferQuestion: Option[EncryptedValue],
                                     name: Option[EncryptedValue],
                                     taxReference: Option[EncryptedValue],
                                     providerAddress: Option[EncryptedValue],
                                     countryCode: Option[EncryptedValue]){

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): TransferPensionScheme =
    TransferPensionScheme(
      ukTransferQuestion = ukTransferQuestion.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      taxReference = taxReference.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      countryCode = countryCode.map(_.decrypted[String])
    )
}

object TransferPensionScheme {
  implicit val format: OFormat[TransferPensionScheme] = Json.format[TransferPensionScheme]
}
