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

package controllers.pensions.transferIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.transferIntoOverseasPensions.routes.{OverseasTransferChargeController, TransferIntoOverseasPensionsCYAController}
import controllers.predicates.actions.ActionsProvider
import forms.TransferPensionSavingsForm.yesNoForm
import models.mongo.PensionsUserData
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.TransferPensionSavingsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TransferPensionSavingsController @Inject()(actionsProvider: ActionsProvider,
                                                 pensionSessionService: PensionSessionService,
                                                 view: TransferPensionSavingsView,
                                                 errorHandler: ErrorHandler)
                                                (implicit cc: MessagesControllerComponents,
                                                 appConfig: AppConfig, clock: Clock)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      val transferPensionSavings = sessionData.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionSavings

      transferPensionSavings match {
        case Some(x) => Future.successful(Ok(view(yesNoForm(sessionData.user).fill(x), taxYear)))
        case None => Future.successful(Ok(view(yesNoForm(sessionData.user), taxYear)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionUserData =>
      yesNoForm(sessionUserData.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        transferPensionSavings => {
          updateSessionData(sessionUserData.pensionsUserData, transferPensionSavings, taxYear)
        }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   transferPensionSavings: Boolean,
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {

    val cyaModel = pensionUserData.pensions
    val updateViewModel = cyaModel.copy(transfersIntoOverseasPensions =
      if (transferPensionSavings) cyaModel.transfersIntoOverseasPensions.copy(transferPensionSavings = Some(transferPensionSavings))
      else TransfersIntoOverseasPensionsViewModel(transferPensionSavings = Some(transferPensionSavings))
    )

    pensionSessionService.createOrUpdateSessionData(request.user,
      updateViewModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(
        if (transferPensionSavings) OverseasTransferChargeController.show(taxYear)
        else TransferIntoOverseasPensionsCYAController.show(taxYear)
      )
    }
  }
}
