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

package services

import models.mongo.PensionsUserData
import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasTransferChargesService @Inject()(pensionSessionService: PensionSessionService)
                                              (implicit ec: ExecutionContext) {


  def updateOverseasTransferChargeQuestion(userData: PensionsUserData, yesNo: Boolean, pensionIndex: Option[Int]): Future[Either[Unit, PensionsUserData]] = {
    val schemes: Seq[TransferPensionScheme] = userData.pensions.transfersIntoOverseasPensions.transferPensionScheme
    val optIndex: Option[Int] = pensionIndex.filter(i => i >= 0 && i < schemes.size)
    val updatedSchemes: Seq[TransferPensionScheme] = {
      optIndex match {
        case Some(index) if schemes(index).ukTransferCharge.getOrElse(false) == yesNo => // if scheme exists and equals prior answer
          schemes.updated(index, schemes(index)) // ensure is the same
        case Some(index) => // if index scheme exists and is different
          schemes.updated(index, TransferPensionScheme(ukTransferCharge = Some(yesNo))) // clear old data
        case _ => // if new or other
          schemes ++ Seq(TransferPensionScheme(ukTransferCharge = Some(yesNo))) // add new to the end
      }
    }
    createOrUpdateModel(userData, userData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = updatedSchemes))
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

