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
                                        ukRefundCharge: Option[Boolean] = None,
                                        name: Option[String] = None,
                                        pstr: Option[String] = None,
                                        qops: Option[String] = None,
                                        providerAddress: Option[String] = None,
                                        alphaTwoCountryCode: Option[String] = None,
                                        alphaThreeCountryCode: Option[String] = None
                                      ) {
  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedOverseasRefundPensionScheme =
    EncryptedOverseasRefundPensionScheme(
      ukRefundCharge = ukRefundCharge.map(_.encrypted),
      name = name.map(_.encrypted),
      pensionSchemeTaxReference = pstr.map(_.encrypted),
      qualifyingRecognisedOverseasPensionScheme = qops.map(_.encrypted),
      providerAddress = providerAddress.map(_.encrypted),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.encrypted),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.encrypted)
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
                                                 alphaTwoCountryCode: Option[EncryptedValue] = None,
                                                 alphaThreeCountryCode: Option[EncryptedValue] = None
                                      ) {
  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): OverseasRefundPensionScheme =
    OverseasRefundPensionScheme(
      ukRefundCharge = ukRefundCharge.map(_.decrypted[Boolean]),
      name = name.map(_.decrypted[String]),
      pstr = pensionSchemeTaxReference.map(_.decrypted[String]),
      qops = qualifyingRecognisedOverseasPensionScheme.map(_.decrypted[String]),
      providerAddress = providerAddress.map(_.decrypted[String]),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.decrypted[String]),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.decrypted[String])
    )
}

object EncryptedOverseasRefundPensionScheme {
  implicit val format: OFormat[EncryptedOverseasRefundPensionScheme] = Json.format[EncryptedOverseasRefundPensionScheme]
}
