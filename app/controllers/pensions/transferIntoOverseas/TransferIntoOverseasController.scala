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

package controllers.pensions.transferIntoOverseas

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import controllers.pensions.routes.OverseasPensionsSummaryController
import forms.TransferIntoOverseasForm.yesNoForm
import forms.YesNoForm
import models.User
import models.mongo.PensionsUserData
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseas.TransferIntoOverseasView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransferIntoOverseasController @Inject()(actionsProvider: ActionsProvider,
                                               pensionSessionService: PensionSessionService,
                                               view: TransferIntoOverseasView,
                                               errorHandler: ErrorHandler
                                              )(implicit cc: MessagesControllerComponents,
                                                appConfig: AppConfig,
                                                clock: Clock,
                                                ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      sessionData.optPensionsUserData match {
        case Some(pensionData) =>
          val transferPensionSavings = pensionData.pensions.transfersIntoOverseasPensions.transferPensionSavings
          transferPensionSavings match {
            case Some(x) => Future.successful(Ok(view(yesNoForm(sessionData.user).fill(x), taxYear)))
            case None => Future.successful(Ok(view(yesNoForm(sessionData.user), taxYear)))
          }
        case None => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionUserData =>
      sessionUserData.optPensionsUserData match {
        case Some(pensionsUserData) =>
          yesNoForm(sessionUserData.user).bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
            transferPensionSavings => {
              updateSessionData(pensionsUserData, transferPensionSavings, taxYear)
            }
          )
        case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   transferPensionSavings: Boolean,
                                   taxYear: Int
                                  )(implicit request: UserSessionDataRequest[T]) = {
    val updatedCyaModel = pensionUserData.pensions.copy(
      transfersIntoOverseasPensions = pensionUserData.pensions.transfersIntoOverseasPensions.copy(
        transferPensionSavings = Some(transferPensionSavings)))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      if(transferPensionSavings) {
        Redirect(controllers.pensions.transferIntoOverseasPension.routes.OverseasTransferChargeController.show(taxYear))
      } else {
        // TODO: Update once `/transfer-charge-summary` Transfer Charge Summary page is available. Redirecting to itself
        Redirect(controllers.pensions.transferIntoOverseas.routes.TransferIntoOverseasController.show(taxYear))
      }
    }
  }
}
