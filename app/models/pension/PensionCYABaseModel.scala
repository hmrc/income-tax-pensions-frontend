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

package models.pension

trait PensionCYABaseModel {
  def isEmpty: Boolean
  def journeyIsNo: Boolean
  def journeyIsUnanswered: Boolean
}

trait PensionRequestSubModel {
  def isEmpty: Boolean
}

trait PensionChargesRequestSubModel extends PensionRequestSubModel

trait PensionIncomeRequestSubModel extends PensionRequestSubModel

trait PensionReliefsRequestSubModel extends PensionRequestSubModel


trait PensionModelRequest {
  def otherSubModelsEmpty[T <: PensionRequestSubModel](subModel: Option[T]): Boolean
  def createSubModel: PensionModelRequest
}