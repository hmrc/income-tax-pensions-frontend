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

case class ShortServiceRefundsViewModel(
                                         shortServiceRefund: Option[Boolean] = None,
                                         shortServiceRefundCharge: Option[BigDecimal] = None,
                                         shortServiceRefundTaxPaid: Option[Boolean] = None,
                                         shortServiceRefundTaxPaidCharge: Option[BigDecimal] = None,
                                         refundPensionScheme: Seq[OverseasRefundPensionScheme] = Nil
                                       ) {
  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedShortServiceRefundsViewModel =
    EncryptedShortServiceRefundsViewModel(
      shortServiceRefund = shortServiceRefund.map(_.encrypted),
      shortServiceRefundCharge = shortServiceRefundCharge.map(_.encrypted),
      shortServiceRefundTaxPaid = shortServiceRefundTaxPaid.map(_.encrypted),
      shortServiceRefundTaxPaidCharge = shortServiceRefundTaxPaidCharge.map(_.encrypted),
      refundPensionScheme = refundPensionScheme.map(_.encrypted())
    )

}

object ShortServiceRefundsViewModel {
  implicit val format: OFormat[ShortServiceRefundsViewModel] = Json.format[ShortServiceRefundsViewModel]
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
                                        ukRefundCharge: Option[Boolean],
                                        name: Option[String],
                                        pensionSchemeTaxReference: Option[String],
                                        qualifyingRecognisedOverseasPensionScheme: Option[String],
                                        providerAddress: Option[String],
                                        countryCode: Option[String]
                                      ) {
  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedOverseasRefundPensionScheme =
    EncryptedOverseasRefundPensionScheme(
      ukRefundCharge = ukRefundCharge.map(_.encrypted),
      name = name.map(_.encrypted),
      pensionSchemeTaxReference = pensionSchemeTaxReference.map(_.encrypted),
      qualifyingRecognisedOverseasPensionScheme = qualifyingRecognisedOverseasPensionScheme.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      countryCode = countryCode.map(_.encrypted)
    )
}

object OverseasRefundPensionScheme {
  implicit val format: OFormat[OverseasRefundPensionScheme] = Json.format[OverseasRefundPensionScheme]
}

case class EncryptedOverseasRefundPensionScheme(
                                        ukRefundCharge: Option[EncryptedValue] = None,
                                        name: Option[EncryptedValue] = None,
                                        pensionSchemeTaxReference: Option[EncryptedValue] = None,
                                        qualifyingRecognisedOverseasPensionScheme: Option[EncryptedValue] = None,
                                        providerAddress: Option[EncryptedValue] = None,
                                        countryCode: Option[EncryptedValue] = None
                                      ) {
  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): OverseasRefundPensionScheme =
    OverseasRefundPensionScheme(
      ukRefundCharge = ukRefundCharge.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      pensionSchemeTaxReference = pensionSchemeTaxReference.map(_.decrypted[String]),
      qualifyingRecognisedOverseasPensionScheme = qualifyingRecognisedOverseasPensionScheme.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      countryCode = countryCode.map(_.decrypted[String])
    )
}

object EncryptedOverseasRefundPensionScheme {
  implicit val format: OFormat[EncryptedOverseasRefundPensionScheme] = Json.format[EncryptedOverseasRefundPensionScheme]
}
