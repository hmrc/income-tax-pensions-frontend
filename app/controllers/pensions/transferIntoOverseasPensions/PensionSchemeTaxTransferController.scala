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
import controllers.pensions.transferIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.TransfersIntoOverseasPensionsRedirects.redirectForSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.pensionSchemeTaxTransferChargeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeTaxTransferController @Inject()(
                                                    actionsProvider: ActionsProvider,
                                                    pensionSessionService: PensionSessionService,
                                                    view: pensionSchemeTaxTransferChargeView,
                                                    formsProvider: FormsProvider,
                                                    errorHandler: ErrorHandler)
                                                  (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      val transferSchemeChargeAmount: Option[BigDecimal] =
        sessionData.pensionsUserData.pensions.transfersIntoOverseasPensions.pensionSchemeTransferChargeAmount
      val transferSchemeCharge: Option[Boolean] = sessionData.pensionsUserData.pensions.transfersIntoOverseasPensions.pensionSchemeTransferCharge
      (transferSchemeCharge, transferSchemeChargeAmount) match {
        case (Some(a), amount) => Future.successful(Ok(view(formsProvider.pensionSchemeTaxTransferForm(sessionData.user).fill((a, amount)), taxYear)))
        case _ => Future.successful(Ok(view(formsProvider.pensionSchemeTaxTransferForm(sessionData.user), taxYear)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      formsProvider.pensionSchemeTaxTransferForm(sessionData.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNoAmount => {
          (yesNoAmount._1, yesNoAmount._2) match {
            case (true, amount) => updateSessionData(sessionData.pensionsUserData, yesNo = true, amount, taxYear)
            case (false, _) => updateSessionData(sessionData.pensionsUserData, yesNo = false, None, taxYear)
          }
        }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      transfersIntoOverseasPensions = pensionUserData.pensions.transfersIntoOverseasPensions.copy(
        pensionSchemeTransferCharge = Some(yesNo),
        pensionSchemeTransferChargeAmount = amount))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {

      if (yesNo) Redirect(redirectForSchemeLoop(schemes = updatedCyaModel.transfersIntoOverseasPensions.transferPensionScheme, taxYear))
      else Redirect(TransferIntoOverseasPensionsCYAController.show(taxYear))
    }
  }
}
