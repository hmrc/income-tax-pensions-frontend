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

case class UnauthorisedPaymentsViewModel(unauthorisedPaymentsQuestion: Option[Boolean] = None,
                                         surchargeQuestion: Option[Boolean] = None,
                                         noSurchargeQuestion: Option[Boolean] = None,
                                         surchargeAmount: Option[BigDecimal] = None,
                                         surchargeTaxAmountQuestion: Option[Boolean] = None,
                                         surchargeTaxAmount: Option[BigDecimal] = None,
                                         noSurchargeAmount: Option[BigDecimal] = None,
                                         noSurchargeTaxAmountQuestion: Option[Boolean] = None,
                                         noSurchargeTaxAmount: Option[BigDecimal] = None,
                                         ukPensionSchemesQuestion: Option[Boolean] = None,
                                         pensionSchemeTaxReference: Option[Seq[String]] = None) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedUnauthorisedPaymentsViewModel =
    EncryptedUnauthorisedPaymentsViewModel(
      unauthorisedPaymentsQuestion = unauthorisedPaymentsQuestion.map(_.encrypted),
      surchargeQuestion = surchargeQuestion.map(_.encrypted),
      noSurchargeQuestion = noSurchargeQuestion.map(_.encrypted),
      surchargeAmount = surchargeAmount.map(_.encrypted),
      surchargeTaxAmountQuestion = surchargeTaxAmountQuestion.map(_.encrypted),
      surchargeTaxAmount = surchargeTaxAmount.map(_.encrypted),
      noSurchargeAmount = noSurchargeAmount.map(_.encrypted),
      noSurchargeTaxAmountQuestion = noSurchargeTaxAmountQuestion.map(_.encrypted),
      noSurchargeTaxAmount = noSurchargeTaxAmount.map(_.encrypted),
      fromUkPensionSchemeQuestion = ukPensionSchemesQuestion.map(_.encrypted),
      pensionSchemeTaxReference = pensionSchemeTaxReference.map(_.map(_.encrypted))
    )
}

object UnauthorisedPaymentsViewModel {
  implicit val format: OFormat[UnauthorisedPaymentsViewModel] = Json.format[UnauthorisedPaymentsViewModel]
}

case class EncryptedUnauthorisedPaymentsViewModel(unauthorisedPaymentsQuestion: Option[EncryptedValue] = None,
                                                  surchargeQuestion: Option[EncryptedValue] = None,
                                                  noSurchargeQuestion: Option[EncryptedValue] = None,
                                                  surchargeAmount: Option[EncryptedValue] = None,
                                                  surchargeTaxAmountQuestion: Option[EncryptedValue] = None,
                                                  surchargeTaxAmount: Option[EncryptedValue] = None,
                                                  noSurchargeAmount: Option[EncryptedValue] = None,
                                                  noSurchargeTaxAmountQuestion: Option[EncryptedValue] = None,
                                                  noSurchargeTaxAmount: Option[EncryptedValue] = None,
                                                  fromUkPensionSchemeQuestion: Option[EncryptedValue],
                                                  pensionSchemeTaxReference: Option[Seq[EncryptedValue]] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): UnauthorisedPaymentsViewModel =
    UnauthorisedPaymentsViewModel(
      unauthorisedPaymentsQuestion = unauthorisedPaymentsQuestion.map(_.decrypted[Boolean]),
      surchargeQuestion = surchargeQuestion.map(_.decrypted[Boolean]),
      noSurchargeQuestion = noSurchargeQuestion.map(_.decrypted[Boolean]),
      surchargeAmount = surchargeAmount.map(_.decrypted[BigDecimal]),
      surchargeTaxAmountQuestion = surchargeTaxAmountQuestion.map(_.decrypted[Boolean]),
      surchargeTaxAmount = surchargeTaxAmount.map(_.decrypted[BigDecimal]),
      noSurchargeAmount = noSurchargeAmount.map(_.decrypted[BigDecimal]),
      noSurchargeTaxAmountQuestion = noSurchargeTaxAmountQuestion.map(_.decrypted[Boolean]),
      noSurchargeTaxAmount = noSurchargeTaxAmount.map(_.decrypted[BigDecimal]),
      ukPensionSchemesQuestion = fromUkPensionSchemeQuestion.map(_.decrypted[Boolean]),
      pensionSchemeTaxReference = pensionSchemeTaxReference.map(_.map(_.decrypted[String]))
    )
}

object EncryptedUnauthorisedPaymentsViewModel {
  implicit val format: OFormat[EncryptedUnauthorisedPaymentsViewModel] = Json.format[EncryptedUnauthorisedPaymentsViewModel]
}
