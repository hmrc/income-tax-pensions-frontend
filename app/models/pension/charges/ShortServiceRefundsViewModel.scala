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

import cats.implicits.{catsSyntaxList, catsSyntaxOptionId, catsSyntaxTuple2Semigroupal, catsSyntaxTuple4Semigroupal, none}
import models.mongo.TextAndKey
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.Constants.zero
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class ShortServiceRefundsViewModel(shortServiceRefund: Option[Boolean] = None,
                                        shortServiceRefundCharge: Option[BigDecimal] = None,
                                        shortServiceRefundTaxPaid: Option[Boolean] = None,
                                        shortServiceRefundTaxPaidCharge: Option[BigDecimal] = None,
                                        refundPensionScheme: Seq[OverseasRefundPensionScheme] = Nil)
    extends PensionCYABaseModel {

  def isEmpty: Boolean =
    shortServiceRefund.isEmpty &&
      shortServiceRefundCharge.isEmpty &&
      shortServiceRefundTaxPaid.isEmpty &&
      shortServiceRefundTaxPaidCharge.isEmpty &&
      refundPensionScheme.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean =
    shortServiceRefund.exists { bool =>
      if (bool)
        shortServiceRefundCharge.isDefined &&
        shortServiceRefundTaxPaid.exists(bool => if (bool) shortServiceRefundTaxPaidCharge.isDefined else true) &&
        refundPensionScheme.nonEmpty &&
        refundPensionScheme.forall(rps => rps.isFinished)
      else true
    }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedShortServiceRefundsViewModel =
    EncryptedShortServiceRefundsViewModel(
      shortServiceRefund = shortServiceRefund.map(_.encrypted),
      shortServiceRefundCharge = shortServiceRefundCharge.map(_.encrypted),
      shortServiceRefundTaxPaid = shortServiceRefundTaxPaid.map(_.encrypted),
      shortServiceRefundTaxPaidCharge = shortServiceRefundTaxPaidCharge.map(_.encrypted),
      refundPensionScheme = refundPensionScheme.map(_.encrypted())
    )

  def toDownstreamRequestModel: OverseasPensionContributions = OverseasPensionContributions(
    shortServiceRefund = shortServiceRefundCharge.getOrElse(0.00),
    shortServiceRefundTaxPaid = shortServiceRefundTaxPaidCharge.getOrElse(0.00),
    overseasSchemeProvider = fromTransferPensionScheme(refundPensionScheme)
  )

  def maybeToDownstreamModel: Option[OverseasPensionContributions] =
    (maybeFromORPS(refundPensionScheme), shortServiceRefundCharge).tupled
      .map { case (schemes, refund) =>
        OverseasPensionContributions(
          overseasSchemeProvider = schemes,
          shortServiceRefund = refund,
          shortServiceRefundTaxPaid = shortServiceRefundTaxPaidCharge.getOrElse(zero)
        )
      }

  private def maybeFromORPS(schemes: Seq[OverseasRefundPensionScheme]): Option[Seq[OverseasSchemeProvider]] =
    schemes.toList.toNel
      .flatMap {
        _.traverse { s =>
          (
            s.name,
            s.providerAddress,
            s.qualifyingRecognisedOverseasPensionScheme,
            s.alphaThreeCountryCode
          ).mapN { case (name, address, qops, countryCode) =>
            OverseasSchemeProvider(
              providerName = name,
              providerAddress = address,
              providerCountryCode = countryCode,
              qualifyingRecognisedOverseasPensionScheme = Seq(enforceValidQopsPrefix(qops)).some,
              pensionSchemeTaxReference = none[Seq[String]] // Not a valid field for this journey
            )
          }
        }
      }
      .map(_.toList.toSeq)

  private def fromTransferPensionScheme(scheme: Seq[OverseasRefundPensionScheme]): Seq[OverseasSchemeProvider] =
    scheme.map(x =>
      OverseasSchemeProvider(
        providerName = x.name.getOrElse(""),
        qualifyingRecognisedOverseasPensionScheme =
          if (x.qualifyingRecognisedOverseasPensionScheme.nonEmpty) Some(Seq(s"Q${x.qualifyingRecognisedOverseasPensionScheme.get}")) else None,
        pensionSchemeTaxReference = None,
        providerAddress = x.providerAddress.getOrElse(""),
        providerCountryCode = x.alphaThreeCountryCode.getOrElse("")
      ))

  override def journeyIsNo: Boolean = this.shortServiceRefund.contains(false)

  override def journeyIsUnanswered: Boolean = this.isEmpty

}

object ShortServiceRefundsViewModel {
  implicit val format: OFormat[ShortServiceRefundsViewModel] = Json.format[ShortServiceRefundsViewModel]

  val empty: ShortServiceRefundsViewModel = ShortServiceRefundsViewModel()
}

case class EncryptedShortServiceRefundsViewModel(
    shortServiceRefund: Option[EncryptedValue] = None,
    shortServiceRefundCharge: Option[EncryptedValue] = None,
    shortServiceRefundTaxPaid: Option[EncryptedValue] = None,
    shortServiceRefundTaxPaidCharge: Option[EncryptedValue] = None,
    refundPensionScheme: Seq[EncryptedOverseasRefundPensionScheme] = Nil
) {
  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel(
      shortServiceRefund = shortServiceRefund.map(_.decrypted[Boolean]),
      shortServiceRefundCharge = shortServiceRefundCharge.map(_.decrypted[BigDecimal]),
      shortServiceRefundTaxPaid = shortServiceRefundTaxPaid.map(_.decrypted[Boolean]),
      shortServiceRefundTaxPaidCharge = shortServiceRefundTaxPaidCharge.map(_.decrypted[BigDecimal]),
      refundPensionScheme = refundPensionScheme.map(_.decrypted())
    )
}

object EncryptedShortServiceRefundsViewModel {
  implicit val format: OFormat[EncryptedShortServiceRefundsViewModel] = Json.format[EncryptedShortServiceRefundsViewModel]
}

case class OverseasRefundPensionScheme(
    name: Option[String] = None,
    qualifyingRecognisedOverseasPensionScheme: Option[String] = None,
    providerAddress: Option[String] = None,
    alphaTwoCountryCode: Option[String] = None,
    alphaThreeCountryCode: Option[String] = None
) {

  // Why do we have alpha 2 and 3 country codes?
  def isFinished: Boolean =
    name.isDefined &&
      providerAddress.isDefined &&
      qualifyingRecognisedOverseasPensionScheme.isDefined &&
      alphaTwoCountryCode.isDefined &&
      alphaThreeCountryCode.isDefined

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedOverseasRefundPensionScheme =
    EncryptedOverseasRefundPensionScheme(
      name = name.map(_.encrypted),
      qualifyingRecognisedOverseasPensionScheme = qualifyingRecognisedOverseasPensionScheme.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.encrypted),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.encrypted)
    )
}

object OverseasRefundPensionScheme {
  implicit val format: OFormat[OverseasRefundPensionScheme] = Json.format[OverseasRefundPensionScheme]

  def allSchemesFinished(schemes: Seq[OverseasRefundPensionScheme]): Boolean =
    schemes.forall(_.isFinished)
}

case class EncryptedOverseasRefundPensionScheme(
    ukRefundCharge: Option[EncryptedValue] = None,
    name: Option[EncryptedValue] = None,
    pensionSchemeTaxReference: Option[EncryptedValue] = None,
    qualifyingRecognisedOverseasPensionScheme: Option[EncryptedValue] = None,
    providerAddress: Option[EncryptedValue] = None,
    alphaTwoCountryCode: Option[EncryptedValue] = None,
    alphaThreeCountryCode: Option[EncryptedValue] = None
) {
  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): OverseasRefundPensionScheme =
    OverseasRefundPensionScheme(
      name = name.map(_.decrypted[String]),
      qualifyingRecognisedOverseasPensionScheme = qualifyingRecognisedOverseasPensionScheme.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.decrypted[String]),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.decrypted[String])
    )
}

object EncryptedOverseasRefundPensionScheme {
  implicit val format: OFormat[EncryptedOverseasRefundPensionScheme] = Json.format[EncryptedOverseasRefundPensionScheme]
}
