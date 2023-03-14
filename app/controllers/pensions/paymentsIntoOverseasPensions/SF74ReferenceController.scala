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
import controllers.predicates.ActionsProvider
import forms.FormsProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.SF74ReferenceView
import javax.inject.{Inject, Singleton}
import models.mongo.PensionsUserData
import models.requests.UserSessionDataRequest

import scala.concurrent.Future

@Singleton
class SF74ReferenceController @Inject()(
                                         actionsProvider: ActionsProvider,
                                         pensionSessionService: PensionSessionService,
                                         view: SF74ReferenceView,
                                         formsProvider: FormsProvider,
                                         errorHandler: ErrorHandler
                                       ) (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper{

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit userSessionDataRequest =>
    val sf74Reference = userSessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.sf74Reference
    sf74Reference match {
      case Some(value) => Future.successful(Ok(view(formsProvider.sf74ReferenceIdForm.fill(value), taxYear)))
      case None => Future.successful(Ok(view(formsProvider.sf74ReferenceIdForm, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      formsProvider.sf74ReferenceIdForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        sf74Reference =>  updateSessionData(userSessionDataRequest.pensionsUserData, Some(sf74Reference), taxYear)
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   sf74Reference : Option[String],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]) = {
    val updatedCyaModel = pensionUserData.pensions.copy(
      paymentsIntoOverseasPensions = pensionUserData.pensions.paymentsIntoOverseasPensions.copy(
        sf74Reference = sf74Reference
      )
    )

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      //TODO: Redirect to the pension scheme details page
        Redirect(controllers.pensions.paymentsIntoOverseasPensions.routes.SF74ReferenceController.show(taxYear))
    }
  }
}