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

package controllers.pensions.lifetimeAllowances

import config.{AppConfig, ErrorHandler}
import controllers.pensions.lifetimeAllowances.routes._
import controllers.pensions.lifetimeAllowances.{routes => lifetimeRoutes}
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionLifetimeAllowancesViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.lifetimeAllowances.AboveAnnualLifetimeAllowanceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AboveAnnualLifetimeAllowanceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       view: AboveAnnualLifetimeAllowanceView,
                                                       appConfig: AppConfig,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"lifetimeAllowance.aboveAnnualLifetimeAllowance.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion match {
          case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
          case None => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
        }
      case None =>
        Future.successful(Redirect(lifetimeRoutes.LifetimeAllowanceCYAController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PensionLifetimeAllowancesViewModel = pensionsCYAModel.pensionLifetimeAllowances
            val updatedCyaModel: PensionsCYAModel = pensionsCYAModel.copy(pensionLifetimeAllowances = {
              if (yesNo) viewModel.copy(aboveLifetimeAllowanceQuestion = Some(true))
              else PensionLifetimeAllowancesViewModel(aboveLifetimeAllowanceQuestion = Some(false))
            })
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(
                if (yesNo) PensionLumpSumController.show(taxYear)
                else LifetimeAllowanceCYAController.show(taxYear)
              )
            }
          case _ => Future.successful(Redirect(LifetimeAllowanceCYAController.show(taxYear)))
        }
      }
    )
  }
}
