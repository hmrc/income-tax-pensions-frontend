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

import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShortServiceRefundsService @Inject()(pensionSessionService: PensionSessionService)
                                          (implicit ec: ExecutionContext) {


  def createOrUpdateShortServiceRefundQuestion(userData: PensionsUserData,
                                               question: Boolean,
                                               pensionIndex: Option[Int]): Future[Either[Unit, PensionsUserData]] = {
    val refundPensionScheme = pensionIndex match {
      case Some(index) => userData.pensions.shortServiceRefunds.refundPensionScheme(index)
      case None => OverseasRefundPensionScheme()
    }
    val model = userData.pensions.shortServiceRefunds
    val updatedModel: ShortServiceRefundsViewModel =
      pensionIndex match {
        case Some(index) =>
          model.copy(refundPensionScheme = model.refundPensionScheme.updated(index, refundPensionScheme.copy(ukRefundCharge = Some(question))))
        case None =>
          if(model.refundPensionScheme.isEmpty) {
            model.copy(refundPensionScheme = Seq(refundPensionScheme.copy(ukRefundCharge = Some(question))))
          }
          else {
            model.refundPensionScheme.last.name match{
              case Some(_) => model.copy(
                refundPensionScheme = model.refundPensionScheme ++ Seq(refundPensionScheme.copy(ukRefundCharge = Some(question))))
              case None => model.copy(
                refundPensionScheme = model.refundPensionScheme.updated(
                  model.refundPensionScheme.size - 1, refundPensionScheme.copy(ukRefundCharge = Some(question))))
            }
          }
      }
    createOrUpdateModel(userData, updatedModel)
  }

  def updateCyaWithShortServiceRefundGatewayQuestion(pensionUserData: PensionsUserData,
                                      yesNo: Boolean,
                                      amount: Option[BigDecimal]): PensionsCYAModel = {
    if(yesNo) {
      pensionUserData.pensions.copy(
        shortServiceRefunds = pensionUserData.pensions.shortServiceRefunds.copy(
          shortServiceRefund = Some(yesNo),
          shortServiceRefundCharge = amount))
    } else {
      clearShortServiceRefunds(pensionUserData)
    }
  }

  private def clearShortServiceRefunds(pensionsUserData: PensionsUserData): PensionsCYAModel =
    pensionsUserData.pensions.copy(
      shortServiceRefunds = ShortServiceRefundsViewModel(shortServiceRefund = Option(false))
    )

  private def createOrUpdateModel(originalUserData: PensionsUserData, updatedModel: ShortServiceRefundsViewModel)
  : Future[Either[Unit, PensionsUserData]] = {

    val updatedCYA = originalUserData.pensions.copy(shortServiceRefunds = updatedModel)
    val updatedUserData = originalUserData.copy(pensions = updatedCYA)

    pensionSessionService.createOrUpdateSessionData(updatedUserData).map {
      case Left(_) => Left(())
      case Right(_) => Right(updatedUserData)
    }
  }


}


