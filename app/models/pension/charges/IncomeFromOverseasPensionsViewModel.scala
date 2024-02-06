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

import forms.Countries
import models.mongo.TextAndKey
import models.pension.PensionCYABaseModel
import models.pension.income.ForeignPension
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion: Option[Boolean] = None,
                                               overseasIncomePensionSchemes: Seq[PensionScheme] = Nil)
    extends PensionCYABaseModel {

  def isEmpty: Boolean = paymentsFromOverseasPensionsQuestion.isEmpty && overseasIncomePensionSchemes.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def isFinished: Boolean =
    paymentsFromOverseasPensionsQuestion.exists(x =>
      if (!x) true else overseasIncomePensionSchemes.nonEmpty && overseasIncomePensionSchemes.forall(_.isFinished))

  def journeyIsNo: Boolean = !paymentsFromOverseasPensionsQuestion.getOrElse(true) && overseasIncomePensionSchemes.isEmpty

  def journeyIsUnanswered: Boolean = this.productIterator.forall(_ == None)

  def hasPriorData: Boolean = paymentsFromOverseasPensionsQuestion.exists(_ && overseasIncomePensionSchemes.nonEmpty)

  def toForeignPension: Seq[ForeignPension] =
    overseasIncomePensionSchemes.map { scheme =>
      ForeignPension(
        countryCode = Countries.get3AlphaCodeFrom2AlphaCode(scheme.alphaTwoCode).get, // TODO validate CYA story to ensure country code present
        taxableAmount = scheme.taxableAmount.get,                                     // TODO validate CYA story to ensure taxable amount is present
        amountBeforeTax = scheme.pensionPaymentAmount,
        taxTakenOff = scheme.pensionPaymentTaxPaid,
        specialWithholdingTax = scheme.specialWithholdingTaxAmount,
        foreignTaxCreditRelief = scheme.foreignTaxCreditReliefQuestion
      )
    }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedIncomeFromOverseasPensionsViewModel =
    EncryptedIncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensionsQuestion = paymentsFromOverseasPensionsQuestion.map(_.encrypted),
      overseasPensionSchemes = overseasIncomePensionSchemes.map(_.encrypted())
    )
}

object IncomeFromOverseasPensionsViewModel {
  implicit val format: OFormat[IncomeFromOverseasPensionsViewModel] = Json.format[IncomeFromOverseasPensionsViewModel]
}

case class EncryptedIncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion: Option[EncryptedValue] = None,
                                                        overseasPensionSchemes: Seq[EncryptedPensionSchemeSummary] = Nil) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): IncomeFromOverseasPensionsViewModel =
    IncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensionsQuestion = paymentsFromOverseasPensionsQuestion.map(_.decrypted[Boolean]),
      overseasPensionSchemes.map(_.decrypted()))
}

object EncryptedIncomeFromOverseasPensionsViewModel {
  implicit val format: OFormat[EncryptedIncomeFromOverseasPensionsViewModel] = Json.format[EncryptedIncomeFromOverseasPensionsViewModel]
}

case class EncryptedPensionSchemeSummary(
    alphaThreeCode: Option[EncryptedValue] = None,
    alphaTwoCode: Option[EncryptedValue] = None,
    pensionPaymentAmount: Option[EncryptedValue] = None,
    pensionPaymentTaxPaid: Option[EncryptedValue] = None,
    specialWithholdingTaxQuestion: Option[EncryptedValue] = None,
    specialWithholdingTaxAmount: Option[EncryptedValue] = None,
    foreignTaxCredit: Option[EncryptedValue] = None,
    taxableAmount: Option[EncryptedValue] = None
) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): PensionScheme =
    PensionScheme(
      alphaThreeCode = alphaThreeCode.map(_.decrypted[String]),
      alphaTwoCode = alphaTwoCode.map(_.decrypted[String]),
      pensionPaymentAmount = pensionPaymentAmount.map(_.decrypted[BigDecimal]),
      pensionPaymentTaxPaid = pensionPaymentTaxPaid.map(_.decrypted[BigDecimal]),
      specialWithholdingTaxQuestion = specialWithholdingTaxQuestion.map(_.decrypted[Boolean]),
      specialWithholdingTaxAmount = specialWithholdingTaxAmount.map(_.decrypted[BigDecimal]),
      foreignTaxCreditReliefQuestion = foreignTaxCredit.map(_.decrypted[Boolean]),
      taxableAmount = taxableAmount.map(_.decrypted[BigDecimal])
    )

}

object EncryptedPensionSchemeSummary {
  implicit val format: OFormat[EncryptedPensionSchemeSummary] = Json.format[EncryptedPensionSchemeSummary]
}

case class PensionScheme(alphaThreeCode: Option[String] = None,
                         alphaTwoCode: Option[String] = None,
                         pensionPaymentAmount: Option[BigDecimal] = None,
                         pensionPaymentTaxPaid: Option[BigDecimal] = None,
                         specialWithholdingTaxQuestion: Option[Boolean] = None,
                         specialWithholdingTaxAmount: Option[BigDecimal] = None,
                         foreignTaxCreditReliefQuestion: Option[Boolean] = None,
                         taxableAmount: Option[BigDecimal] = None) {

  private def resetTaxableAmountIfDifferent(newPensionScheme: PensionScheme) =
    if (newPensionScheme == this) this else newPensionScheme.copy(taxableAmount = None)

  def updateFTCR(newForeignTaxCreditReliefQuestion: Boolean): PensionScheme = {
    val updatedPensionScheme = copy(
      foreignTaxCreditReliefQuestion = Some(newForeignTaxCreditReliefQuestion)
    )
    resetTaxableAmountIfDifferent(updatedPensionScheme)
  }

  def updatePensionPayment(newPensionPaymentAmount: Option[BigDecimal], newPensionPaymentTaxPaid: Option[BigDecimal]): PensionScheme = {
    val updatedPensionScheme = copy(
      pensionPaymentAmount = newPensionPaymentAmount,
      pensionPaymentTaxPaid = newPensionPaymentTaxPaid
    )
    resetTaxableAmountIfDifferent(updatedPensionScheme)
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedPensionSchemeSummary =
    EncryptedPensionSchemeSummary(
      alphaThreeCode = alphaThreeCode.map(_.encrypted),
      alphaTwoCode = alphaTwoCode.map(_.encrypted),
      pensionPaymentAmount = pensionPaymentAmount.map(_.encrypted),
      pensionPaymentTaxPaid = pensionPaymentTaxPaid.map(_.encrypted),
      specialWithholdingTaxQuestion = specialWithholdingTaxQuestion.map(_.encrypted),
      specialWithholdingTaxAmount = specialWithholdingTaxAmount.map(_.encrypted),
      foreignTaxCredit = foreignTaxCreditReliefQuestion.map(_.encrypted),
      taxableAmount = taxableAmount.map(_.encrypted)
    )

  def isFinished: Boolean =
    this.alphaTwoCode.isDefined &&
      this.pensionPaymentAmount.isDefined &&
      this.pensionPaymentTaxPaid.isDefined &&
      this.specialWithholdingTaxQuestion.exists(value => !value || (value && this.specialWithholdingTaxAmount.nonEmpty)) &&
      foreignTaxCreditReliefQuestion.isDefined &&
      taxableAmount.isDefined
}

object PensionScheme {
  implicit val format: OFormat[PensionScheme] = Json.format[PensionScheme]

  def calcTaxableAmount(pensionPaymentAmount: Option[BigDecimal],
                        pensionPaymentTaxPaid: Option[BigDecimal],
                        foreignTaxCreditReliefQuestion: Option[Boolean]): Option[BigDecimal] =
    for {
      amountBeforeTax <- pensionPaymentAmount
      nonUkTaxPaid    <- pensionPaymentTaxPaid
      isFtcr          <- foreignTaxCreditReliefQuestion
      taxableAmount =
        if (isFtcr) {
          amountBeforeTax
        } else {
          amountBeforeTax - nonUkTaxPaid
        }
    } yield taxableAmount
}
