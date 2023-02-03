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
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowances.routes.{AboveReducedAnnualAllowanceController, ReducedAnnualAllowanceTypeController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowance.ReducedAnnualAllowanceView

import javax.inject.Inject
import scala.concurrent.Future

class ReducedAnnualAllowanceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 reducedAnnualAllowanceView: ReducedAnnualAllowanceView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport {

  def yesNoForm(implicit user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"annualAllowance.reducedAnnualAllowance.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
          case Some(question) => Future.successful(Ok(reducedAnnualAllowanceView(yesNoForm(request.user).fill(question), taxYear)))
          case None => Future.successful(Ok(reducedAnnualAllowanceView(yesNoForm(request.user), taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(reducedAnnualAllowanceView(formWithErrors, taxYear))),
      yesNo => {
        //todo verify what should happen if no data is present. Should be redirected to CYA page.
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel = pensionsCYAModel.pensionsAnnualAllowances

            val updatedCyaModel = pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(
              reducedAnnualAllowanceQuestion = Some(yesNo),
              moneyPurchaseAnnualAllowance = if (yesNo) viewModel.moneyPurchaseAnnualAllowance else None,
              taperedAnnualAllowance = if (yesNo) viewModel.taperedAnnualAllowance else None
            ))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              if (yesNo) {
                Redirect(ReducedAnnualAllowanceTypeController.show(taxYear))
              } else {
                Redirect(AboveReducedAnnualAllowanceController.show(taxYear))
              }
            }
        }
      }
    )
  }
}
