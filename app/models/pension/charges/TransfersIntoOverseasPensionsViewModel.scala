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

import cats.implicits.{catsSyntaxOptionId, catsSyntaxTuple3Semigroupal, none, toTraverseOps}
import models.mongo.TextAndKey
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.Constants.GBAlpha3Code
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
      if (x) {
        overseasTransferCharge.exists(x =>
          if (!x) {
            true
          } else {
            overseasTransferChargeAmount.isDefined &&
            pensionSchemeTransferCharge.exists(x =>
              if (!x) {
                true
              } else {
                pensionSchemeTransferChargeAmount.isDefined &&
                transferPensionScheme.nonEmpty && transferPensionScheme.forall(tps => tps.isFinished)
              })
          })
      } else {
        true
      })

  def maybeToDownstreamRequestModel: Option[PensionSchemeOverseasTransfers] =
    (transferPensionSavings, overseasTransferCharge, pensionSchemeTransferCharge)
      .flatMapN { case (transferred, charged, schemePaidTax) =>
        if (transferred && charged && schemePaidTax)
          for {
            osp           <- fromTransferPensionSchemes(transferPensionScheme)
            charge        <- overseasTransferChargeAmount
            chargeTaxPaid <- pensionSchemeTransferChargeAmount
          } yield PensionSchemeOverseasTransfers(
            overseasSchemeProvider = osp,
            transferCharge = charge,
            transferChargeTaxPaid = chargeTaxPaid
          )
        else none[PensionSchemeOverseasTransfers]
      }

  private def fromTransferPensionSchemes(allTps: Seq[TransferPensionScheme]): Option[Seq[OverseasSchemeProvider]] =
    allTps.traverse { tps =>
      for {
        name    <- tps.name
        address <- tps.providerAddress
      } yield OverseasSchemeProvider(
        providerName = name,
        providerAddress = address,
        providerCountryCode = tps.alphaThreeCountryCode.fold(ifEmpty = GBAlpha3Code)(identity),
        qualifyingRecognisedOverseasPensionScheme = tps.qops.fold(ifEmpty = none[Seq[String]])(qops => Seq(enforceValidQopsPrefix(qops)).some),
        pensionSchemeTaxReference = tps.pstr.fold(ifEmpty = none[Seq[String]])(Seq(_).some)
      )
    }

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
}

object TransfersIntoOverseasPensionsViewModel {
  implicit val format: OFormat[TransfersIntoOverseasPensionsViewModel] = Json.format[TransfersIntoOverseasPensionsViewModel]

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
                                 pstr: Option[String] = None,
                                 qops: Option[String] = None,
                                 providerAddress: Option[String] = None,
                                 alphaTwoCountryCode: Option[String] = None,
                                 alphaThreeCountryCode: Option[String] = None) {

  def isFinished: Boolean =
    this.name.isDefined && this.providerAddress.isDefined &&
      this.ukTransferCharge.exists(x =>
        if (x) {
          this.pstr.isDefined
        } else {
          this.qops.isDefined && this.alphaThreeCountryCode.isDefined
        })

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTransferPensionScheme =
    EncryptedTransferPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.encrypted),
      name = name.map(_.encrypted),
      pensionSchemeTaxReference = pstr.map(_.encrypted),
      qualifyingRecognisedOverseasPensionScheme = qops.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.encrypted),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.encrypted)
    )
}

case class EncryptedTransferPensionScheme(ukTransferCharge: Option[EncryptedValue],
                                          name: Option[EncryptedValue],
                                          pensionSchemeTaxReference: Option[EncryptedValue],
                                          qualifyingRecognisedOverseasPensionScheme: Option[EncryptedValue],
                                          providerAddress: Option[EncryptedValue],
                                          alphaTwoCountryCode: Option[EncryptedValue],
                                          alphaThreeCountryCode: Option[EncryptedValue]) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): TransferPensionScheme =
    TransferPensionScheme(
      ukTransferCharge = ukTransferCharge.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      pstr = pensionSchemeTaxReference.map(_.decrypted[String]),
      qops = qualifyingRecognisedOverseasPensionScheme.map(_.decrypted[String]),
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
