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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.ActionsProvider
import forms.TransferPensionSavingsForm.yesNoForm
import models.mongo.PensionsUserData
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.TransferPensionSavingsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TransferPensionSavingsController @Inject() (actionsProvider: ActionsProvider,
                                                  pensionSessionService: PensionSessionService,
                                                  view: TransferPensionSavingsView,
                                                  errorHandler: ErrorHandler,
                                                  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val transferPensionSavings = request.sessionData.pensions.transfersIntoOverseasPensions.transferPensionSavings

    transferPensionSavings match {
      case Some(x) => Future.successful(Ok(view(yesNoForm(request.user).fill(x), taxYear)))
      case None    => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    yesNoForm(request.user)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        transferPensionSavings => updateSessionData(request.sessionData, transferPensionSavings, taxYear)
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, transferPensionSavings: Boolean, taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {

    val cyaModel = pensionUserData.pensions
    val updateViewModel = cyaModel.copy(transfersIntoOverseasPensions = if (transferPensionSavings) {
      cyaModel.transfersIntoOverseasPensions.copy(transferPensionSavings = Some(transferPensionSavings))
    } else {
      TransfersIntoOverseasPensionsViewModel(transferPensionSavings = Some(transferPensionSavings))
    })

    pensionSessionService.createOrUpdateSessionData(request.user, updateViewModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(
        if (transferPensionSavings) routes.OverseasTransferChargeController.show(taxYear)
        else routes.TransferIntoOverseasPensionsCYAController.show(TaxYear(taxYear))
      )
    }
  }
}
