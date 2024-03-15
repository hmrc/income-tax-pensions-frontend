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
import controllers.pensions.paymentsIntoOverseasPensions.routes.ReliefsSchemeDetailsController
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.SF74ReferencePage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.SF74ReferenceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SF74ReferenceController @Inject() (actionsProvider: ActionsProvider,
                                         pensionSessionService: PensionSessionService,
                                         view: SF74ReferenceView,
                                         formsProvider: FormsProvider,
                                         errorHandler: ErrorHandler,
                                         mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, reliefIndex, SF74ReferencePage, taxYear) { relief: Relief =>
      relief.sf74Reference match {
        case Some(value) => Future.successful(Ok(view(formsProvider.sf74ReferenceIdForm.fill(value), taxYear, reliefIndex)))
        case None        => Future.successful(Ok(view(formsProvider.sf74ReferenceIdForm, taxYear, reliefIndex)))
      }
    }
  }

  def submit(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, reliefIndex, SF74ReferencePage, taxYear) { _: Relief =>
      formsProvider.sf74ReferenceIdForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, reliefIndex))),
          sf74Reference => updateSessionData(request.sessionData, Some(sf74Reference), taxYear, reliefIndex.get)
        )
    }
  }

  private def updateSessionData[T](pensionsUserData: PensionsUserData, sf74Reference: Option[String], taxYear: Int, idx: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {

    val piopUserData = pensionsUserData.pensions.paymentsIntoOverseasPensions

    val updatedCyaModel = pensionsUserData.pensions.copy(
      paymentsIntoOverseasPensions =
        piopUserData.copy(reliefs = piopUserData.reliefs.updated(idx, piopUserData.reliefs(idx).copy(sf74Reference = sf74Reference)))
    )
    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionsUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(ReliefsSchemeDetailsController.show(taxYear, Some(idx)))
    }
  }

}
