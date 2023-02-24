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

package controllers.pensions.transferIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import controllers.validateIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.TransferPensionScheme
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.RemoveTransferChargeSchemeView
import routes._
import javax.inject.Inject
import scala.concurrent.Future

class RemoveTransferChargeSchemeController @Inject()(actionsProvider: ActionsProvider,
                                                     pensionSessionService: PensionSessionService,
                                                     view: RemoveTransferChargeSchemeView, errorHandler: ErrorHandler)
                                                    (implicit val mcc: MessagesControllerComponents,
                                                     appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val transferChargeScheme = sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme
    validateIndex(index, transferChargeScheme.size).fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))) {
      i =>
        transferChargeScheme(i).name.fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))){
          name => Future.successful(Ok(view(taxYear, name, index)))
        }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val transferChargeScheme = sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme
    validateIndex(index, transferChargeScheme.size)
      .fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))) {
        i =>
          val updatedTransferScheme = transferChargeScheme.patch(i, Nil, 1)
          updateSessionData(sessionUserData.pensionsUserData, updatedTransferScheme, taxYear)
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   transferChargeScheme: Seq[TransferPensionScheme],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]) = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      transfersIntoOverseasPensions = pensionUserData.pensions.transfersIntoOverseasPensions.copy(
        transferPensionScheme = transferChargeScheme))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(TransferChargeSummaryController.show(taxYear))
    }
  }


}