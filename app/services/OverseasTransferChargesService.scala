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

import models.User
import models.mongo.PensionsUserData
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasTransferChargesService @Inject()(pensionSessionService: PensionSessionService)
                                              (implicit ec: ExecutionContext) {


  def updateOverseasTransferChargeQuestion(userData: PensionsUserData, question: Boolean): Future[Either[Unit, PensionsUserData]] = {
    val model = if (question) userData.pensions.transfersIntoOverseasPensions else TransfersIntoOverseasPensionsViewModel()
    val updatedModel = model.copy(transfersIntoOverseas = Some(question))
    createOrUpdateModel(userData, updatedModel)
  }

  private def createOrUpdateModel(originalUserData: PensionsUserData, updatedModel: TransfersIntoOverseasPensionsViewModel)
  : Future[Either[Unit, PensionsUserData]] = {

    val updatedCYA = originalUserData.pensions.copy(transfersIntoOverseasPensions = updatedModel)
    val updatedUserData = originalUserData.copy(pensions = updatedCYA)

    pensionSessionService.createOrUpdateSessionData(updatedUserData).map {
      case Left(_) => Left(())
      case Right(_) => Right(updatedUserData)
    }
  }


}

