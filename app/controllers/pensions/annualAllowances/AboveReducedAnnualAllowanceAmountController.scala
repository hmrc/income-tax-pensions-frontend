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
import controllers.pensions.annualAllowances.routes.{AboveReducedAnnualAllowanceController, ReducedAnnualAllowanceController}
import controllers.pensions.lifetimeAllowances.routes.PensionProviderPaidTaxController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{AmountForm, FormUtils}
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.annualAllowances.AboveReducedAnnualAllowanceAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AboveReducedAnnualAllowanceAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                            authAction: AuthorisedAction,
                                                            view: AboveReducedAnnualAllowanceAmountView,
                                                            appConfig: AppConfig,
                                                            pensionSessionService: PensionSessionService,
                                                            errorHandler: ErrorHandler,
                                                            clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm(reducedAnnualAllowanceQuestion: Boolean, user: User): Form[BigDecimal] = {
    val (emptyFieldKey, wrongFormatKey, exceedsMaxAmountKey) =
      if (reducedAnnualAllowanceQuestion) {
        (
          s"pensions.reducedAnnualAllowanceAmount.reduced.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
          s"pensions.reducedAnnualAllowanceAmount.reduced.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
          s"pensions.reducedAnnualAllowanceAmount.reduced.error.overMaximum.${if (user.isAgent) "agent" else "individual"}"
        )
      } else {
        (
          "pensions.reducedAnnualAllowanceAmount.nonReduced.error.noEntry",
          "pensions.reducedAnnualAllowanceAmount.nonReduced.error.incorrectFormat",
          "pensions.reducedAnnualAllowanceAmount.nonReduced.error.overMaximum"
        )
      }
    AmountForm.amountForm(
      emptyFieldKey = emptyFieldKey,
      wrongFormatKey = wrongFormatKey,
      exceedsMaxAmountKey = exceedsMaxAmountKey
    )
  }

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
          case Some(reducedAnnualAllowanceQuestion) =>
            if (data.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion.contains(true)) {
              data.pensions.pensionsAnnualAllowances.aboveAnnualAllowance match {
                case Some(amount) =>
                  Future.successful(Ok(view(amountForm(reducedAnnualAllowanceQuestion, request.user).fill(amount), taxYear, reducedAnnualAllowanceQuestion)))
                case None => Future.successful(Ok(view(amountForm(reducedAnnualAllowanceQuestion, request.user), taxYear, reducedAnnualAllowanceQuestion)))
              }
            } else {
              Future.successful(Redirect(AboveReducedAnnualAllowanceController.show(taxYear)))
            }
          case None => Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
        }
      case _ =>
        //TODO: redirect to the annual allowances CYA page when available
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      data =>
        val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
        pensionsCYAModel.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
          case Some(reducedAnnualAllowanceQuestion) =>
            amountForm(reducedAnnualAllowanceQuestion, request.user).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, reducedAnnualAllowanceQuestion))),
              amount => {
                val viewModel: PensionAnnualAllowancesViewModel = pensionsCYAModel.pensionsAnnualAllowances
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(aboveAnnualAllowance = Some(amount)))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
                  Redirect(PensionProviderPaidTaxController.show(taxYear))
                }
              }
            )
          case None => Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
        }
    }
  }
}
