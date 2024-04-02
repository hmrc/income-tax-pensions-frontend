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

package controllers.pensions

import models.mongo.PensionsUserData
import models.pension.statebenefits.IncomeFromPensionsViewModel
import models.requests.UserSessionDataRequest
import play.api.mvc.AnyContent

package object incomeFromPensions {

  def areStatePensionClaimsComplete(answers: IncomeFromPensionsViewModel): Boolean =
    answers.statePension.exists(_.isFinished) &&
      answers.statePensionLumpSum.exists(_.isFinished)

  def refreshSessionModel(updatedJourney: IncomeFromPensionsViewModel)(implicit request: UserSessionDataRequest[AnyContent]): PensionsUserData = {
    val updatedPensions = request.sessionData.pensions.copy(incomeFromPensions = updatedJourney)
    request.sessionData.copy(pensions = updatedPensions)
  }
}
