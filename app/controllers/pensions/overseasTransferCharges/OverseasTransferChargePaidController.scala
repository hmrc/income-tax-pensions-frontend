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

package controllers.pensions.overseasTransferCharges

import config.{AppConfig, ErrorHandler}
import controllers.pensions.overseasTransferCharges.routes._
import controllers.predicates.ActionsProvider
import models.mongo.PensionsUserData
import models.pension.pages.OverseasTransferChargePaidPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{OverseasTransferChargesService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.overseasTransferCharges.OverseasTransferChargesPaidView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasTransferChargePaidController @Inject()(actionsProvider: ActionsProvider,
                                                     formsProvider: FormsProvider,
                                                     pageView: OverseasTransferChargesPaidView,
                                                     errorHandler: ErrorHandler,
                                                     overseasTransferChargesService: OverseasTransferChargesService
                                                    )(implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit sessionUserData =>
    Ok(
      pageView(OverseasTransferChargePaidPage(taxYear, sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions, formsProvider.overseasTransferChargePaidForm)))
  }

  def submit(taxYear: Int): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit request =>
      formsProvider.overseasTransferChargePaidForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(pageView(OverseasTransferChargePaidPage(taxYear, request.pensionsUserData.pensions.transfersIntoOverseasPensions, formWithErrors)))),
        yesNoValue => {
          overseasTransferChargesService.updateOverseasTransferChargeQuestion(request.pensionsUserData, yesNoValue).map {
            case Left(_) => errorHandler.internalServerError()
            case Right(userData) => Redirect(getRedirectCall(taxYear, yesNoValue, userData))
          }
        }
      )
    }
  }


  private def getRedirectCall(taxYear: Int,
                              yesNoValue: Boolean,
                              userData: PensionsUserData): Call = {
    if (yesNoValue) {
      controllers.pensions.overseasTransferCharges.routes.OverseasTransferChargePaidController.show(taxYear) //Redirect to
    } else {
      controllers.pensions.overseasTransferCharges.routes.OverseasTransferChargePaidController.show(taxYear) //Redirect to
    }
  }
}
