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
import utils.DecryptorInstances.booleanDecryptor
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.booleanEncryptor
import utils.{EncryptedValue, SecureGCMCipher}

case class IncomeFromPensionsViewModel(statePension: Option[StateBenefitViewModel] = None,
                                       statePensionLumpSum: Option[StateBenefitViewModel] = None,
                                       uKPensionIncomesQuestion: Option[Boolean] = None,
                                       uKPensionIncomes: Seq[UkPensionIncomeViewModel] = Seq.empty) {

  def isEmpty: Boolean =
    statePension.isEmpty && statePensionLumpSum.isEmpty && uKPensionIncomesQuestion.isEmpty && uKPensionIncomes.isEmpty

  def isFinishedStatePension: Boolean = statePension.exists(_.isFinished) && statePensionLumpSum.exists(_.isFinished)

  def isFinishedUkPension: Boolean = uKPensionIncomesQuestion.exists(x => !x || uKPensionIncomes.forall(_.isFinished))

  def journeyIsNoStatePension: Boolean =
    statePension.exists(!_.amountPaidQuestion.getOrElse(true)) && statePensionLumpSum.exists(!_.amountPaidQuestion.getOrElse(true))

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedIncomeFromPensionsViewModel =
    EncryptedIncomeFromPensionsViewModel(
      statePension = statePension.map(_.encrypted()),
      statePensionLumpSum = statePensionLumpSum.map(_.encrypted()),
      uKPensionIncomesQuestion = uKPensionIncomesQuestion.map(_.encrypted),
      uKPensionIncomes = uKPensionIncomes.map(_.encrypted())
    )
}

object IncomeFromPensionsViewModel {
  implicit val format: OFormat[IncomeFromPensionsViewModel] = Json.format[IncomeFromPensionsViewModel]
}

case class EncryptedIncomeFromPensionsViewModel(
                                       statePension: Option[EncryptedStateBenefitViewModel] = None,
                                       statePensionLumpSum: Option[EncryptedStateBenefitViewModel] = None,
                                       uKPensionIncomesQuestion: Option[EncryptedValue] = None,
                                       uKPensionIncomes: Seq[EncryptedUkPensionIncomeViewModel] = Seq.empty) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): IncomeFromPensionsViewModel =
    IncomeFromPensionsViewModel(
      statePension = statePension.map(_.decrypted()),
      statePensionLumpSum = statePensionLumpSum.map(_.decrypted()),
      uKPensionIncomesQuestion = uKPensionIncomesQuestion.map(_.decrypted[Boolean]),
      uKPensionIncomes.map(_.decrypted())
    )
}

object EncryptedIncomeFromPensionsViewModel {
  implicit val format: OFormat[EncryptedIncomeFromPensionsViewModel] = Json.format[EncryptedIncomeFromPensionsViewModel]
}
