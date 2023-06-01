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

package services

import models.APIErrorModel
import models.pension.{PensionCYABaseModel, PensionModelRequest, PensionRequestSubModel}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


trait PensionConnectorHelper[SubModel <: PensionRequestSubModel, ModelRequest <: PensionModelRequest] {

  def saveData(nino: String, taxYear: Int, model: ModelRequest)
              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]]

  def deleteData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]]
  
  def sendDownstream(nino: String,
                     taxYear: Int,
                     subModel: Option[SubModel],
                     cya: Option[PensionCYABaseModel],
                     requestModel: ModelRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]] = {

    val otherModels = requestModel.otherSubModelsEmpty(subModel)

    def isSubModelEmpty(subModel: Option[SubModel]): Boolean = {
      subModel.exists(_.isEmpty)
    }

    if (cya.exists(_.journeyIsNo) || cya.exists(_.journeyIsUnanswered)) {
      (otherModels, subModel.isEmpty || isSubModelEmpty(subModel)) match {
        case (true, true) =>
          //Do nothing or delete
          deleteData(nino, taxYear)
        case (true, false) =>
          //delete
          //this is an issue, because once NO is selected, the sub model should not contain any data and should be empty
          deleteData(nino, taxYear)
        case (false, true) =>
          // Put or do nothing
          saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[ModelRequest])
        case (false, false) =>
          //Put
          //this is an issue, because once NO is selected, the sub model should not contain any data and should be empty
          saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[ModelRequest])
      }
    } else {
      saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[ModelRequest])
    }
  }
}