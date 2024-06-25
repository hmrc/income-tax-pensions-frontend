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
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, localDateDecryptor, uuidDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, localDateEncryptor, uuidEncryptor}
import utils.{AesGCMCrypto, EncryptedValue}

import java.time.LocalDate
import java.util.UUID

case class StateBenefitViewModel(benefitId: Option[UUID] = None,
                                 startDateQuestion: Option[Boolean] = None,
                                 startDate: Option[LocalDate] = None,
                                 amountPaidQuestion: Option[Boolean] = None,
                                 amount: Option[BigDecimal] = None,
                                 taxPaidQuestion: Option[Boolean] = None,
                                 taxPaid: Option[BigDecimal] = None) {

  def void: StateBenefitViewModel = StateBenefitViewModel()

  def isEmpty: Boolean = this.productIterator.forall(_ == None)

  def isFinished: Boolean = {
    val notClaiming = amountPaidQuestion.contains(false)

    // Fields common across the state pension and state pension lump sum journeys
    val areCommonAnswered = amount.isDefined &&
      startDateQuestion.isDefined &&
      startDate.isDefined

    val areClaimingLumpSum = taxPaidQuestion.contains(true)

    val isLumpSumFinished =
      if (areClaimingLumpSum) taxPaid.isDefined
      else true

    val claimIsFinished = areCommonAnswered && isLumpSumFinished

    notClaiming || claimIsFinished
  }

  def encrypted()(implicit aesGCMCrypto: AesGCMCrypto, textAndKey: TextAndKey): EncryptedStateBenefitViewModel =
    EncryptedStateBenefitViewModel(
      benefitId = benefitId.map(_.encrypted),
      startDateQuestion = startDateQuestion.map(_.encrypted),
      startDate = startDate.map(_.encrypted),
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

  def decrypted()(implicit aesGCMCrypto: AesGCMCrypto, textAndKey: TextAndKey): StateBenefitViewModel =
    StateBenefitViewModel(
      benefitId = benefitId.map(_.decrypted[UUID]),
      startDateQuestion = startDateQuestion.map(_.decrypted[Boolean]),
      startDate = startDate.map(_.decrypted[LocalDate]),
      amountPaidQuestion = amountPaidQuestion.map(_.decrypted[Boolean]),
      amount = amount.map(_.decrypted[BigDecimal]),
      taxPaidQuestion = taxPaidQuestion.map(_.decrypted[Boolean]),
      taxPaid = taxPaid.map(_.decrypted[BigDecimal])
    )
}

object EncryptedStateBenefitViewModel {
  implicit val format: OFormat[EncryptedStateBenefitViewModel] = Json.format[EncryptedStateBenefitViewModel]
}
