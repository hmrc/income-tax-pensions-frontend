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

package controllers.pensions.annualAllowance

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowance.routes.AboveReducedAnnualAllowanceAmountController
import controllers.pensions.annualAllowance.routes.ReducedAnnualAllowanceController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowance.AboveReducedAnnualAllowanceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AboveReducedAnnualAllowanceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      aboveReducedAnnualAllowanceView: AboveReducedAnnualAllowanceView,
                                                      appConfig: AppConfig,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      clock: Clock) extends FrontendController(cc) with I18nSupport {


  def yesNoForm(user: User, reducedAnnualAllowanceQuestion: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.aboveReducedAnnualAllowance.${if (reducedAnnualAllowanceQuestion) "reduced" else "nonReduced"}." +
      s"error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
          case Some(reducedAnnualAllowanceQuestion) =>
            data.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion match {
              case Some(value) => Future.successful(Ok(aboveReducedAnnualAllowanceView(
                yesNoForm(request.user, reducedAnnualAllowanceQuestion).fill(value), taxYear, reducedAnnualAllowanceQuestion)))
              case None =>
                Future.successful(Ok(aboveReducedAnnualAllowanceView(
                  yesNoForm(request.user, reducedAnnualAllowanceQuestion), taxYear, reducedAnnualAllowanceQuestion)))
            }
          case None =>
            Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
        }
      case None =>
        //TODO: navigate to annual allowance CYA when available
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      data =>
        val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
        pensionsCYAModel.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
          case Some(reducedAnnualAllowanceQuestion) =>
            yesNoForm(request.user, reducedAnnualAllowanceQuestion).bindFromRequest.fold(
              formWithErrors => Future.successful(BadRequest(aboveReducedAnnualAllowanceView(formWithErrors, taxYear, reducedAnnualAllowanceQuestion))),
              yesNo => {
                val viewModel: PensionAnnualAllowancesViewModel = pensionsCYAModel.pensionsAnnualAllowances
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(
                    aboveAnnualAllowanceQuestion = Some(yesNo),
                    aboveAnnualAllowance = if (yesNo) viewModel.aboveAnnualAllowance else None))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
                  if (yesNo) {
                    //TODO redirect to Above your annual allowance amount page
                    Redirect(AboveReducedAnnualAllowanceAmountController.show(taxYear))
                  } else {
                    //TODO redirect to check your annual allowance page
                    Redirect(PensionsSummaryController.show(taxYear))
                  }
                }
              }
            )
        }
    }
  }
}
