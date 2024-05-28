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

import connectors.OptionalContentHttpReads
import models.mongo.{PensionsCYAModel, TextAndKey}
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class TransfersIntoOverseasPensionsViewModel(transferPensionSavings: Option[Boolean] = None,
                                                  overseasTransferCharge: Option[Boolean] = None,
                                                  overseasTransferChargeAmount: Option[BigDecimal] = None,
                                                  pensionSchemeTransferCharge: Option[Boolean] = None,
                                                  pensionSchemeTransferChargeAmount: Option[BigDecimal] = None,
                                                  transferPensionScheme: Seq[TransferPensionScheme] = Nil)
    extends PensionCYABaseModel {

  def isEmpty: Boolean = transferPensionSavings.isEmpty && overseasTransferCharge.isEmpty && overseasTransferChargeAmount.isEmpty &&
    pensionSchemeTransferCharge.isEmpty && pensionSchemeTransferChargeAmount.isEmpty && transferPensionScheme.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean =
    transferPensionSavings.exists(x =>
      !x || overseasTransferCharge.exists(x =>
        !x || overseasTransferChargeAmount.isDefined && pensionSchemeTransferCharge.exists(x =>
          !x || pensionSchemeTransferChargeAmount.isDefined && transferPensionScheme.nonEmpty && transferPensionScheme.forall(tps =>
            tps.isFinished))))

  def journeyIsNo: Boolean = this.transferPensionSavings.contains(false)

  def journeyIsUnanswered: Boolean = this.isEmpty

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransfersIntoOverseasPensionsViewModel =
    EncryptedTransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = transferPensionSavings.map(_.encrypted),
      overseasTransferCharge = overseasTransferCharge.map(_.encrypted),
      overseasTransferChargeAmount = overseasTransferChargeAmount.map(_.encrypted),
      pensionSchemeTransferCharge = pensionSchemeTransferCharge.map(_.encrypted),
      pensionSchemeTransferChargeAmount = pensionSchemeTransferChargeAmount.map(_.encrypted),
      transferPensionScheme = transferPensionScheme.map(_.encrypted())
    )

  def toPensionsCYAModel: PensionsCYAModel = PensionsCYAModel.emptyModels.copy(transfersIntoOverseasPensions = this)
}

object TransfersIntoOverseasPensionsViewModel {
  implicit val format: OFormat[TransfersIntoOverseasPensionsViewModel] = Json.format[TransfersIntoOverseasPensionsViewModel]
  implicit val optRds: OptionalContentHttpReads[TransfersIntoOverseasPensionsViewModel] =
    new OptionalContentHttpReads[TransfersIntoOverseasPensionsViewModel]

  val empty: TransfersIntoOverseasPensionsViewModel = TransfersIntoOverseasPensionsViewModel()
}

case class EncryptedTransfersIntoOverseasPensionsViewModel(transferPensionSavings: Option[EncryptedValue] = None,
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

object EncryptedTransfersIntoOverseasPensionsViewModel {
  implicit val format: OFormat[EncryptedTransfersIntoOverseasPensionsViewModel] = Json.format[EncryptedTransfersIntoOverseasPensionsViewModel]
}

case class TransferPensionScheme(ukTransferCharge: Option[Boolean] = None,
                                 name: Option[String] = None,
                                 schemeReference: Option[String] = None,
                                 providerAddress: Option[String] = None,
                                 alphaTwoCountryCode: Option[String] = None,
                                 alphaThreeCountryCode: Option[String] = None) {

  def isFinished: Boolean =
    name.isDefined && providerAddress.isDefined && ukTransferCharge.isDefined && schemeReference.isDefined && alphaThreeCountryCode.isDefined

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransferPensionScheme =
    EncryptedTransferPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.encrypted),
      name = name.map(_.encrypted),
      schemeReference = schemeReference.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.encrypted),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.encrypted)
    )
}

case class EncryptedTransferPensionScheme(ukTransferCharge: Option[EncryptedValue],
                                          name: Option[EncryptedValue],
                                          schemeReference: Option[EncryptedValue],
                                          providerAddress: Option[EncryptedValue],
                                          alphaTwoCountryCode: Option[EncryptedValue],
                                          alphaThreeCountryCode: Option[EncryptedValue]) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): TransferPensionScheme =
    TransferPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      schemeReference = schemeReference.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.decrypted[String]),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.decrypted[String])
    )
}

object TransferPensionScheme {
  implicit val format: OFormat[TransferPensionScheme] = Json.format[TransferPensionScheme]
}

object EncryptedTransferPensionScheme {
  implicit val format: OFormat[EncryptedTransferPensionScheme] = Json.format[EncryptedTransferPensionScheme]
}
