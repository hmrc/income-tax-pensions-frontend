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
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, instantDecryptor, localDateDecryptor, uuidDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, instantEncryptor, localDateEncryptor, uuidEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.{Instant, LocalDate}
import java.util.UUID

// Why do we have all of these fields? Not all of them are questions on the form. Do we send them downstream?
case class StateBenefitViewModel(benefitId: Option[UUID] = None,
                                 startDateQuestion: Option[Boolean] = None,
                                 startDate: Option[LocalDate] = None,
                                 endDateQuestion: Option[Boolean] = None,
                                 endDate: Option[LocalDate] = None,
                                 submittedOnQuestion: Option[Boolean] = None,
                                 submittedOn: Option[Instant] = None,
                                 dateIgnoredQuestion: Option[Boolean] = None,
                                 dateIgnored: Option[Instant] = None,
                                 amountPaidQuestion: Option[Boolean] = None,
                                 amount: Option[BigDecimal] = None,
                                 taxPaidQuestion: Option[Boolean] = None,
                                 taxPaid: Option[BigDecimal] = None) {

  def void: StateBenefitViewModel = StateBenefitViewModel()

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def isFinished: Boolean = {
    val noClaim = amountPaidQuestion.contains(false)

    // Fields common across the state pension and state pension lump sum journeys
    val areCommonAnswered = amount.isDefined &&
      startDateQuestion.isDefined &&
      startDate.isDefined

    val areClaimingLumpSum = taxPaidQuestion.contains(true)

    val isLumpSumFinished =
      if (areClaimingLumpSum) taxPaid.isDefined
      else true

    noClaim || areCommonAnswered && isLumpSumFinished
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedStateBenefitViewModel =
    EncryptedStateBenefitViewModel(
      benefitId = benefitId.map(_.encrypted),
      startDateQuestion = startDateQuestion.map(_.encrypted),
      startDate = startDate.map(_.encrypted),
      endDateQuestion = endDateQuestion.map(_.encrypted),
      endDate = endDate.map(_.encrypted),
      submittedOnQuestion = submittedOnQuestion.map(_.encrypted),
      submittedOn = submittedOn.map(_.encrypted),
      dateIgnoredQuestion = dateIgnoredQuestion.map(_.encrypted),
      dateIgnored = dateIgnored.map(_.encrypted),
      amountPaidQuestion = amountPaidQuestion.map(_.encrypted),
      amount = amount.map(_.encrypted),
      taxPaidQuestion = taxPaidQuestion.map(_.encrypted),
      taxPaid = taxPaid.map(_.encrypted)
    )
}

object StateBenefitViewModel {
  implicit val format: OFormat[StateBenefitViewModel] = Json.format[StateBenefitViewModel]

  val empty: StateBenefitViewModel = StateBenefitViewModel()
}

case class EncryptedStateBenefitViewModel(
    benefitId: Option[EncryptedValue] = None,
    startDateQuestion: Option[EncryptedValue] = None,
    startDate: Option[EncryptedValue] = None,
    endDateQuestion: Option[EncryptedValue] = None,
    endDate: Option[EncryptedValue] = None,
    submittedOnQuestion: Option[EncryptedValue] = None,
    submittedOn: Option[EncryptedValue] = None,
    dateIgnoredQuestion: Option[EncryptedValue] = None,
    dateIgnored: Option[EncryptedValue] = None,
    amountPaidQuestion: Option[EncryptedValue] = None,
    amount: Option[EncryptedValue] = None,
    taxPaidQuestion: Option[EncryptedValue],
    taxPaid: Option[EncryptedValue] = None,
    addToCalculation: Option[EncryptedValue] = None
) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): StateBenefitViewModel =
    StateBenefitViewModel(
      benefitId = benefitId.map(_.decrypted[UUID]),
      startDateQuestion = startDateQuestion.map(_.decrypted[Boolean]),
      startDate = startDate.map(_.decrypted[LocalDate]),
      endDateQuestion = endDateQuestion.map(_.decrypted[Boolean]),
      endDate = endDate.map(_.decrypted[LocalDate]),
      submittedOnQuestion = submittedOnQuestion.map(_.decrypted[Boolean]),
      submittedOn = submittedOn.map(_.decrypted[Instant]),
      dateIgnoredQuestion = dateIgnoredQuestion.map(_.decrypted[Boolean]),
      dateIgnored = dateIgnored.map(_.decrypted[Instant]),
      amountPaidQuestion = amountPaidQuestion.map(_.decrypted[Boolean]),
      amount = amount.map(_.decrypted[BigDecimal]),
      taxPaidQuestion = taxPaidQuestion.map(_.decrypted[Boolean]),
      taxPaid = taxPaid.map(_.decrypted[BigDecimal])
    )
}

object EncryptedStateBenefitViewModel {
  implicit val format: OFormat[EncryptedStateBenefitViewModel] = Json.format[EncryptedStateBenefitViewModel]
}
