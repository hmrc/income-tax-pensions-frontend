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

package models.pension.statebenefits

import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{AesGCMCrypto, EncryptedValue}

case class UkPensionIncomeViewModel(employmentId: Option[String] = None,
                                    pensionId: Option[String] = None,
                                    startDate: Option[String] = None,
                                    endDate: Option[String] = None,
                                    pensionSchemeName: Option[String] = None,
                                    pensionSchemeRef: Option[String] = None,
                                    amount: Option[BigDecimal] = None,
                                    taxPaid: Option[BigDecimal] = None,
                                    isCustomerEmploymentData: Option[Boolean] = None) {

  def encrypted()(implicit aesGCMCrypto: AesGCMCrypto, textAndKey: TextAndKey): EncryptedUkPensionIncomeViewModel =
    EncryptedUkPensionIncomeViewModel(
      employmentId = employmentId.map(_.encrypted),
      pensionId = pensionId.map(_.encrypted),
      startDate = startDate.map(_.encrypted),
      endDate = endDate.map(_.encrypted),
      pensionSchemeName = pensionSchemeName.map(_.encrypted),
      pensionSchemeRef = pensionSchemeRef.map(_.encrypted),
      amount = amount.map(_.encrypted),
      taxPaid = taxPaid.map(_.encrypted),
      isCustomerEmploymentData = isCustomerEmploymentData.map(_.encrypted)
    )

  def isFinished: Boolean =
    this.pensionId.isDefined &&
      this.startDate.isDefined &&
      this.pensionSchemeName.isDefined &&
      this.pensionSchemeRef.isDefined &&
      this.amount.isDefined &&
      this.taxPaid.isDefined
}

object UkPensionIncomeViewModel {
  implicit val format: OFormat[UkPensionIncomeViewModel] = Json.format[UkPensionIncomeViewModel]
}

case class EncryptedUkPensionIncomeViewModel(employmentId: Option[EncryptedValue] = None,
                                             pensionId: Option[EncryptedValue] = None,
                                             startDate: Option[EncryptedValue] = None,
                                             endDate: Option[EncryptedValue] = None,
                                             pensionSchemeName: Option[EncryptedValue] = None,
                                             pensionSchemeRef: Option[EncryptedValue] = None,
                                             amount: Option[EncryptedValue] = None,
                                             taxPaid: Option[EncryptedValue] = None,
                                             isCustomerEmploymentData: Option[EncryptedValue] = None) {

  def decrypted()(implicit aesGCMCrypto: AesGCMCrypto, textAndKey: TextAndKey): UkPensionIncomeViewModel =
    UkPensionIncomeViewModel(
      employmentId = employmentId.map(_.decrypted[String]),
      pensionId = pensionId.map(_.decrypted[String]),
      startDate = startDate.map(_.decrypted[String]),
      endDate = endDate.map(_.decrypted[String]),
      pensionSchemeName = pensionSchemeName.map(_.decrypted[String]),
      pensionSchemeRef = pensionSchemeRef.map(_.decrypted[String]),
      amount = amount.map(_.decrypted[BigDecimal]),
      taxPaid = taxPaid.map(_.decrypted[BigDecimal]),
      isCustomerEmploymentData = isCustomerEmploymentData.map(_.decrypted[Boolean])
    )
}

object EncryptedUkPensionIncomeViewModel {
  implicit val format: OFormat[EncryptedUkPensionIncomeViewModel] = Json.format[EncryptedUkPensionIncomeViewModel]
}
