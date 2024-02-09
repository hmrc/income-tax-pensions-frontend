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
import models.pension.{PensionCYABaseModel, PensionRequestModel, PensionSubRequestModel}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait PensionConnectorHelper[SubRequestModel <: PensionSubRequestModel, RequestModel <: PensionRequestModel] {

  def saveData(nino: String, taxYear: Int, model: RequestModel)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]]

  def deleteData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]]

  def sendDownstream(nino: String,
                     taxYear: Int,
                     subRequestModel: Option[SubRequestModel],
                     cya: Option[PensionCYABaseModel],
                     requestModel: RequestModel)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[APIErrorModel, Unit]] = {

    val otherModels = requestModel.otherSubRequestModelsEmpty(subRequestModel)

    def isSubModelEmpty(subModel: Option[SubRequestModel]): Boolean =
      subModel.exists(_.isEmpty)

    // TODO - The below code requires refactoring
    if (cya.exists(_.journeyIsNo) || cya.exists(_.journeyIsUnanswered)) {
      (otherModels, subRequestModel.isEmpty || isSubModelEmpty(subRequestModel)) match {
        case (true, true) =>
          // Do nothing or delete
          deleteData(nino, taxYear)
        case (true, false) =>
          // delete
          deleteData(nino, taxYear)
        case (false, true) =>
          // Put or do nothing
          saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[RequestModel])
        case (false, false) =>
          // Put
          saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[RequestModel])
      }
    } else {
      saveData(nino, taxYear, requestModel.createSubModel.asInstanceOf[RequestModel])
    }
  }
}
