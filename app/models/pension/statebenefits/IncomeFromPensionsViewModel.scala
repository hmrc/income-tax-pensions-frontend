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

import connectors.OptionalContentHttpReads
import models.mongo.{PensionsCYAModel, TextAndKey}
import models.pension.{IncomeFromPensionsStatePensionAnswers, PensionCYABaseModel}
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.booleanDecryptor
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.booleanEncryptor
import utils.{EncryptedValue, SecureGCMCipher}

// TODO Any reason why these two journeys are aggregated in one case class?
case class IncomeFromPensionsViewModel(statePension: Option[StateBenefitViewModel] = None,
                                       statePensionLumpSum: Option[StateBenefitViewModel] = None,
                                       uKPensionIncomesQuestion: Option[Boolean] = None,
                                       uKPensionIncomes: Option[List[UkPensionIncomeViewModel]] = None)
    extends PensionCYABaseModel {

  def getUKPensionIncomes: List[UkPensionIncomeViewModel] = uKPensionIncomes.getOrElse(Nil)

  def isEmpty: Boolean =
    statePension.isEmpty &&
      statePensionLumpSum.isEmpty &&
      uKPensionIncomesQuestion.isEmpty &&
      getUKPensionIncomes.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def removeUkPensionIncome: IncomeFromPensionsViewModel = copy(uKPensionIncomesQuestion = None, uKPensionIncomes = None)

  def removeStatePension: IncomeFromPensionsViewModel = copy(statePension = None, statePensionLumpSum = None)

  def toIncomeFromPensionsStatePensionAnswers(sessionId: String): IncomeFromPensionsStatePensionAnswers =
    IncomeFromPensionsStatePensionAnswers(
      statePension = statePension,
      statePensionLumpSum = statePensionLumpSum,
      sessionId = Some(sessionId)
    )

  def isStatePensionFinished: Boolean =
    statePension.exists(_.isFinished) ||
      statePensionLumpSum.exists(_.isFinished)

  def isUkPensionFinished: Boolean =
    uKPensionIncomesQuestion.exists(x => !x || (getUKPensionIncomes.nonEmpty && getUKPensionIncomes.forall(_.isFinished)))

  def isFinished: Boolean = isStatePensionFinished && isUkPensionFinished

  def journeyIsNoStatePension: Boolean =
    statePension.exists(!_.amountPaidQuestion.getOrElse(true)) && statePensionLumpSum.exists(!_.amountPaidQuestion.getOrElse(true))

  private def journeyIsNoUkPension: Boolean = uKPensionIncomesQuestion.exists(x => !x && getUKPensionIncomes.isEmpty)

  def journeyIsNo: Boolean = journeyIsNoStatePension && journeyIsNoUkPension

  def journeyIsUnanswered: Boolean = this.isEmpty

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedIncomeFromPensionsViewModel =
    EncryptedIncomeFromPensionsViewModel(
      statePension = statePension.map(_.encrypted()),
      statePensionLumpSum = statePensionLumpSum.map(_.encrypted()),
      uKPensionIncomesQuestion = uKPensionIncomesQuestion.map(_.encrypted),
      uKPensionIncomes = uKPensionIncomes.map(_.map(_.encrypted()))
    )

  def toPensionsCYAModel: PensionsCYAModel =
    PensionsCYAModel.emptyModels.copy(incomeFromPensions = this)

}

object IncomeFromPensionsViewModel {
  implicit val format: OFormat[IncomeFromPensionsViewModel]                  = Json.format[IncomeFromPensionsViewModel]
  implicit val optRds: OptionalContentHttpReads[IncomeFromPensionsViewModel] = new OptionalContentHttpReads[IncomeFromPensionsViewModel]

}

case class EncryptedIncomeFromPensionsViewModel(statePension: Option[EncryptedStateBenefitViewModel] = None,
                                                statePensionLumpSum: Option[EncryptedStateBenefitViewModel] = None,
                                                uKPensionIncomesQuestion: Option[EncryptedValue] = None,
                                                uKPensionIncomes: Option[List[EncryptedUkPensionIncomeViewModel]] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): IncomeFromPensionsViewModel =
    IncomeFromPensionsViewModel(
      statePension = statePension.map(_.decrypted()),
      statePensionLumpSum = statePensionLumpSum.map(_.decrypted()),
      uKPensionIncomesQuestion = uKPensionIncomesQuestion.map(_.decrypted[Boolean]),
      uKPensionIncomes.map(_.map(_.decrypted()))
    )
}

object EncryptedIncomeFromPensionsViewModel {
  implicit val format: OFormat[EncryptedIncomeFromPensionsViewModel] = Json.format[EncryptedIncomeFromPensionsViewModel]
}
