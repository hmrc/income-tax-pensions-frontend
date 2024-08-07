/*
 * Copyright 2024 HM Revenue & Customs
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

package models.pension

import connectors.OptionalContentHttpReads
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.libs.json.{Json, OFormat}

case class IncomeFromPensionsStatePensionAnswers(
    statePension: Option[StateBenefitViewModel],
    statePensionLumpSum: Option[StateBenefitViewModel],
    sessionId: Option[String]
) {
  def toIncomeFromPensionsViewModel: IncomeFromPensionsViewModel =
    IncomeFromPensionsViewModel(
      statePension = statePension,
      statePensionLumpSum = statePensionLumpSum,
      None,
      None
    )
}

object IncomeFromPensionsStatePensionAnswers {
  implicit val format: OFormat[IncomeFromPensionsStatePensionAnswers] = Json.format[IncomeFromPensionsStatePensionAnswers]
  implicit val optRds: OptionalContentHttpReads[IncomeFromPensionsStatePensionAnswers] =
    new OptionalContentHttpReads[IncomeFromPensionsStatePensionAnswers]

  def empty: IncomeFromPensionsStatePensionAnswers = IncomeFromPensionsStatePensionAnswers(None, None, None)
}
