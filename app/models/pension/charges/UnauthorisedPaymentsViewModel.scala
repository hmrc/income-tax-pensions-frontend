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
import models.pension.PensionCYABaseModel
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class UnauthorisedPaymentsViewModel(surchargeQuestion: Option[Boolean] = None,
                                         noSurchargeQuestion: Option[Boolean] = None,
                                         surchargeAmount: Option[BigDecimal] = None,
                                         surchargeTaxAmountQuestion: Option[Boolean] = None,
                                         surchargeTaxAmount: Option[BigDecimal] = None,
                                         noSurchargeAmount: Option[BigDecimal] = None,
                                         noSurchargeTaxAmountQuestion: Option[Boolean] = None,
                                         noSurchargeTaxAmount: Option[BigDecimal] = None,
                                         ukPensionSchemesQuestion: Option[Boolean] = None,
                                         pensionSchemeTaxReference: Option[Seq[String]] = None)
    extends PensionCYABaseModel {

  private def yesNoAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]): Boolean =
    boolField.exists(value => !value || (value && amountField.nonEmpty))

  def isFinished: Boolean = {
    val isDone_surchargeQuestions = surchargeQuestion.exists(q =>
      !q ||
        surchargeAmount.isDefined && yesNoAndAmountPopulated(surchargeTaxAmountQuestion, surchargeTaxAmount))
    val isDone_noSurchargeQuestions = noSurchargeQuestion.exists(q =>
      !q ||
        noSurchargeAmount.isDefined && yesNoAndAmountPopulated(noSurchargeTaxAmountQuestion, noSurchargeTaxAmount))
    val isDone_pstrQuestions =
      if (isDone_surchargeQuestions || isDone_noSurchargeQuestions) {
        ukPensionSchemesQuestion.exists(q => !q || pensionSchemeTaxReference.nonEmpty)
      } else { true }

    Seq(
      isDone_surchargeQuestions,
      isDone_noSurchargeQuestions,
      isDone_pstrQuestions
    ).forall(x => x)
  }

  def toUnauth: PensionSchemeUnauthorisedPayments = PensionSchemeUnauthorisedPayments( // scalastyle:off simplify.boolean.expression
    pensionSchemeTaxReference = pensionSchemeTaxReference,
    surcharge = if (surchargeQuestion.contains(true) && surchargeAmount.nonEmpty) {
      Some(Charge(this.surchargeAmount.get, surchargeTaxAmount.getOrElse(0)))
    } else {
      None
    },
    noSurcharge = if (noSurchargeQuestion.contains(true) && noSurchargeAmount.nonEmpty) {
      Some(Charge(this.noSurchargeAmount.get, noSurchargeTaxAmount.getOrElse(0)))
    } else {
      None
    }
  ) // scalastyle:on simplify.boolean.expression

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def nonEmpty: Boolean = !isEmpty

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedUnauthorisedPaymentsViewModel =
    EncryptedUnauthorisedPaymentsViewModel(
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

  def unauthorisedPaymentQuestion: Option[Boolean] =
    (noSurchargeQuestion, surchargeQuestion) match {
      case (None, None)               => None
      case (Some(false), Some(false)) => Some(false)
      case (Some(false), None)        => Some(false)
      case (None, Some(false))        => Some(false)
      case _                          => Some(true)
    }

  def copyWithQuestionsApplied(surchargeQuestion: Option[Boolean], noSurchargeQuestion: Option[Boolean]): UnauthorisedPaymentsViewModel = {
    val questionsSet = copy(surchargeQuestion = surchargeQuestion, noSurchargeQuestion = noSurchargeQuestion)
    val surchargeQuestionsApplied =
      if (!surchargeQuestion.getOrElse(false)) {
        questionsSet.copy(surchargeAmount = None, surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
      } else {
        questionsSet
      }
    val noSurchargeQuestionsApplied =
      if (!noSurchargeQuestion.getOrElse(false)) {
        surchargeQuestionsApplied.copy(noSurchargeAmount = None, noSurchargeTaxAmountQuestion = None, noSurchargeTaxAmount = None)
      } else {
        surchargeQuestionsApplied
      }

    val neitherSurchargeQuestionsApplied = // scalastyle:off simplify.boolean.expression
      if (!noSurchargeQuestion.getOrElse(false) && !surchargeQuestion.getOrElse(false)) {
        noSurchargeQuestionsApplied.copy(ukPensionSchemesQuestion = None, pensionSchemeTaxReference = None)
      } // scalastyle:on simplify.boolean.expression
      else {
        noSurchargeQuestionsApplied
      }
    neitherSurchargeQuestionsApplied
  }

  override def journeyIsNo: Boolean =
    (!noSurchargeQuestion.getOrElse(true)
      && !surchargeQuestion.getOrElse(true)
      && surchargeAmount.isEmpty
      && surchargeTaxAmountQuestion.isEmpty
      && surchargeTaxAmount.isEmpty
      && noSurchargeAmount.isEmpty
      && noSurchargeTaxAmountQuestion.isEmpty
      && noSurchargeTaxAmount.isEmpty
      && ukPensionSchemesQuestion.isEmpty
      && pensionSchemeTaxReference.isEmpty)

  override def journeyIsUnanswered: Boolean = this.productIterator.forall(_ == None)
}

object UnauthorisedPaymentsViewModel {
  implicit val format: OFormat[UnauthorisedPaymentsViewModel] = Json.format[UnauthorisedPaymentsViewModel]
}

case class EncryptedUnauthorisedPaymentsViewModel(surchargeQuestion: Option[EncryptedValue] = None,
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
