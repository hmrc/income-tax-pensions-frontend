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

package models.pension.reliefs

import models.IncomeTaxUserData
import models.pension.{PensionRequestModel, PensionSubRequestModel}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdatePensionReliefsModel(pensionReliefs: Reliefs) extends PensionRequestModel {
  override def otherSubRequestModelsEmpty[T <: PensionSubRequestModel](subModel: Option[T]): Boolean = true
  override def createSubModel: PensionRequestModel                                                   = this
}

object CreateUpdatePensionReliefsModel {
  implicit val format: OFormat[CreateUpdatePensionReliefsModel] = Json.format[CreateUpdatePensionReliefsModel]

  def fromPriorData(prior: IncomeTaxUserData): CreateUpdatePensionReliefsModel =
    CreateUpdatePensionReliefsModel(Reliefs.fromPriorData(prior))
}
