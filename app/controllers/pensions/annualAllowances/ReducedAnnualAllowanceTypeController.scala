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
import controllers.pensions.annualAllowances.routes._
import controllers.pensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.ReducedAnnualAllowanceTypeQuestionForm
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowances.ReducedAnnualAllowanceTypeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReducedAnnualAllowanceTypeController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                     appConfig: AppConfig,
                                                     authAction: AuthorisedAction,
                                                     pensionSessionService: PensionSessionService,
                                                     errorHandler: ErrorHandler,
                                                     view: ReducedAnnualAllowanceTypeView,
                                                     clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion.contains(true)) {
          val taperedAnnualAllowance = data.pensions.pensionsAnnualAllowances.taperedAnnualAllowance
          val moneyPurchaseAnnualAllowance = data.pensions.pensionsAnnualAllowances.moneyPurchaseAnnualAllowance
          val form = ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceTypeForm(request.user.isAgent)
          Future.successful(Ok(view(form, taxYear, moneyPurchaseAnnualAllowance, taperedAnnualAllowance)))
        } else {
          Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
        }
      case _ =>
        //TODO: navigate to annual allowance CYA when available
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion.contains(true)) {
          ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceTypeForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
            reducedAllowanceTypeSelections => {

              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val pensionsAnnualAllowances = pensionsCYAModel.pensionsAnnualAllowances

              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(pensionsAnnualAllowances = pensionsAnnualAllowances.copy(
                  moneyPurchaseAnnualAllowance = Some(reducedAllowanceTypeSelections.containsMoneyPurchase),
                  taperedAnnualAllowance = Some(reducedAllowanceTypeSelections.containsTapered)))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(AboveReducedAnnualAllowanceController.show(taxYear))
              }
            }
          )
        } else {
          Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
        }

      case _ =>
        //TODO: navigate to annual allowance CYA when available
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))

    }

  }

}
