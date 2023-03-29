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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{ActionsProvider, AuthorisedAction}
import controllers.validateIndex
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.ReliefSchemeDetailsView

import javax.inject.{Inject, Singleton}

@Singleton
class ReliefsSchemeDetailsController @Inject()(authAction: AuthorisedAction,
                                               view: ReliefSchemeDetailsView,
                                               actionsProvider: ActionsProvider)
                                              (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {
    implicit userSessionDataRequest =>
      validateIndex(reliefIndex, userSessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
        case Some(idx) =>
          Ok(view(taxYear, userSessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx), reliefIndex))
        case _ =>
          Redirect(routes.PensionsCustomerReferenceNumberController.show(taxYear))
      }
  }

  def submit(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {
    implicit userSessionDataRequest =>
      validateIndex(reliefIndex, userSessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
        case Some(idx) =>
          Ok(view(taxYear, userSessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx), reliefIndex))
          //todo redirect to CYA page when complete
        case _ =>
          Redirect(routes.PensionsCustomerReferenceNumberController.show(taxYear))
      }
    }
}