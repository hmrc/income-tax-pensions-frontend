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

package models.session

import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData

final case class PensionCYAMergedWithPriorData(newPensionsCYAModel: PensionsCYAModel, newModelChanged: Boolean)

object PensionCYAMergedWithPriorData {

  /** Notice it will return newModelChanged=true at the beginning when no session exists, so it is initialized initially with empty values.
    */
  def mergeSessionAndPriorData(existingSessionData: Option[PensionsUserData], priorData: Option[AllPensionsData]): PensionCYAMergedWithPriorData = {
    val maybeExistingSessionPension = existingSessionData.map(_.pensions)

    val userSessionFromOnlyPriorData = priorData.map(AllPensionsData.generateCyaFromPrior).getOrElse(PensionsCYAModel.emptyModels)
    val mergedSession                = userSessionFromOnlyPriorData.merge(maybeExistingSessionPension)
    val existingSessionDidNotChanged = maybeExistingSessionPension.exists(_.equals(mergedSession))

    PensionCYAMergedWithPriorData(mergedSession, newModelChanged = !existingSessionDidNotChanged)
  }
}
