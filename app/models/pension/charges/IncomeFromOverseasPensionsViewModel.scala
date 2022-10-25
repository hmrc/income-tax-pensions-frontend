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
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}


case class IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensions: Option[Boolean] = None,
                                               overseasPensionSchemes: Option[Seq[PensionSchemeSummary]] = None) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedIncomeFromOverseasPensionsViewModel =
    EncryptedIncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensions = paymentsFromOverseasPensions.map(_.encrypted),
      overseasPensionSchemes = overseasPensionSchemes.map(_.map(_.encrypted()))
    )
}

object IncomeFromOverseasPensionsViewModel {
  implicit val format: OFormat[IncomeFromOverseasPensionsViewModel] = Json.format[IncomeFromOverseasPensionsViewModel]
}


case class EncryptedIncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensions: Option[EncryptedValue] = None,
                                                        overseasPensionSchemes: Option[Seq[EncryptedPensionSchemeSummary]] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): IncomeFromOverseasPensionsViewModel =
    IncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensions = paymentsFromOverseasPensions.map(_.decrypted[Boolean]),
      overseasPensionSchemes.map(_.map(_.decrypted())))
}

object EncryptedIncomeFromOverseasPensionsViewModel {
  implicit val format: OFormat[EncryptedIncomeFromOverseasPensionsViewModel] = Json.format[EncryptedIncomeFromOverseasPensionsViewModel]
}

case class EncryptedPensionSchemeSummary(
                                          country: Option[EncryptedValue] = None,
                                          pensionPaymentAmount: Option[EncryptedValue] = None,
                                          pensionPaymentTaxPaid: Option[EncryptedValue] = None,
                                          specialWithholdingTaxQuestion: Option[EncryptedValue] = None,
                                          specialWithholdingTaxAmount: Option[EncryptedValue] = None,
                                          foreignTaxCredit: Option[EncryptedValue] = None,
                                          taxableAmount: Option[EncryptedValue] = None
                                        ) {


  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): PensionSchemeSummary =
    PensionSchemeSummary(
      country = country.map(_.decrypted[String]),
      pensionPaymentAmount = pensionPaymentAmount.map(_.decrypted[BigDecimal]),
      pensionPaymentTaxPaid = pensionPaymentTaxPaid.map(_.decrypted[BigDecimal]),
      specialWithholdingTaxQuestion = specialWithholdingTaxQuestion.map(_.decrypted[Boolean]),
      specialWithholdingTaxAmount = specialWithholdingTaxAmount.map(_.decrypted[BigDecimal]),
      foreignTaxCredit = foreignTaxCredit.map(_.decrypted[Boolean]),
      taxableAmount = taxableAmount.map(_.decrypted[BigDecimal])
    )

}

object EncryptedPensionSchemeSummary {
  implicit val format: OFormat[EncryptedPensionSchemeSummary] = Json.format[EncryptedPensionSchemeSummary]
}


case class PensionSchemeSummary(
                                 country: Option[String] = None,
                                 pensionPaymentAmount: Option[BigDecimal] = None,
                                 pensionPaymentTaxPaid: Option[BigDecimal] = None,
                                 specialWithholdingTaxQuestion: Option[Boolean] = None,
                                 specialWithholdingTaxAmount: Option[BigDecimal] = None,
                                 foreignTaxCredit: Option[Boolean] = None,
                                 taxableAmount: Option[BigDecimal] = None
                               ) {
  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedPensionSchemeSummary = {
    EncryptedPensionSchemeSummary(
      country = country.map(_.encrypted),
      pensionPaymentAmount = pensionPaymentAmount.map(_.encrypted),
      pensionPaymentTaxPaid = pensionPaymentTaxPaid.map(_.encrypted),
      specialWithholdingTaxQuestion = specialWithholdingTaxQuestion.map(_.encrypted),
      specialWithholdingTaxAmount = specialWithholdingTaxAmount.map(_.encrypted),
      foreignTaxCredit = foreignTaxCredit.map(_.encrypted),
      taxableAmount = taxableAmount.map(_.encrypted)
    )
  }
}

object PensionSchemeSummary {
  implicit val format: OFormat[PensionSchemeSummary] = Json.format[PensionSchemeSummary]
}
