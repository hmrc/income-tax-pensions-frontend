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

package controllers.pensions.annualAllowances

import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowances.ReducedAnnualAllowanceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReducedAnnualAllowanceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 actionsProvider: ActionsProvider,
                                                 reducedAnnualAllowanceView: ReducedAnnualAllowanceView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 formsProvider: FormsProvider,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport {
  def show(taxYear: Int): Action[AnyContent] =  actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val yesNoForm = formsProvider.reducedAnnualAllowanceForm(request.user)
    request.pensionsUserData.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
      case Some(question) => Future.successful(Ok(reducedAnnualAllowanceView(yesNoForm.fill(question), taxYear)))
      case None => Future.successful(Ok(reducedAnnualAllowanceView(yesNoForm, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    formsProvider.reducedAnnualAllowanceForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(reducedAnnualAllowanceView(formWithErrors, taxYear))),
      yesNo => updateSessionData(request.pensionsUserData, yesNo, taxYear)
    )
  }
  
  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   reducedAnnualAllowanceQ: Boolean, taxYear: Int)
                                  (implicit request: UserSessionDataRequest[T]) = {
    
    val updatedCyaModel = pensionUserData.pensions.copy(
      pensionsAnnualAllowances = pensionUserData.pensions.pensionsAnnualAllowances.copy(Some(reducedAnnualAllowanceQ))
    )
    pensionSessionService.createOrUpdateSessionData(
      request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(
        if (reducedAnnualAllowanceQ) routes.ReducedAnnualAllowanceTypeController.show(taxYear) else routes.AnnualAllowanceCYAController.show(taxYear)
      )
    }
  }
}
