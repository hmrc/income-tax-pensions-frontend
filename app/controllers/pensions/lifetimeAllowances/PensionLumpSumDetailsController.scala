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
import controllers.predicates.TaxYearAction.taxYearAction
import controllers.predicates.AuthorisedAction
import forms.{FormUtils, TupleAmountForm}
import models.mongo.PensionsCYAModel
import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.lifetimeAllowances.PensionLumpSumDetailsView
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.lifetimeAllowances.routes.PensionLumpSumController


import javax.inject.Inject
import scala.concurrent.Future

class PensionLumpSumDetailsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                pensionLumpSumDetailsView: PensionLumpSumDetailsView,
                                                appConfig: AppConfig,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler,
                                                clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm(isAgent: Boolean): Form[(BigDecimal, BigDecimal)] = TupleAmountForm.amountForm(
    emptyFieldKey1 = s"lifetimeAllowance.pensionLumpSumDetails.beforeTax.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey1 = s"lifetimeAllowance.pensionLumpSumDetails.beforeTax.error.incorrectFormat",
    exceedsMaxAmountKey1 = s"common.beforeTax.error.overMaximum",
    emptyFieldKey2 = s"lifetimeAllowance.pensionLumpSumDetails.taxPaid.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey2 = s"common.taxPaid.error.incorrectFormat",
    exceedsMaxAmountKey2 = s"common.taxPaid.error.overMaximum"
  )


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>

        val totalTaxOpt = data.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.amount)
        val taxPaidOpt = data.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.taxPaid)

        (totalTaxOpt, taxPaidOpt) match {
          case (Some(totalTax), Some(taxPaid)) =>
            Future.successful(Ok(pensionLumpSumDetailsView(amountForm(request.user.isAgent).fill((totalTax, taxPaid)), taxYear)))
          case (_, _) =>
            Future.successful(Ok(pensionLumpSumDetailsView(amountForm(request.user.isAgent), taxYear)))
        }
      case _ =>
        //TODO: - Redirect to Annual Lifetime allowances cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionLumpSumDetailsView(formWithErrors, taxYear))),
      amounts => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            if (data.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion.contains(true)) {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: PensionLifetimeAllowancesViewModel = pensionsCYAModel.pensionLifetimeAllowances
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  pensionLifetimeAllowances = viewModel.copy(
                    pensionAsLumpSum = Some(LifetimeAllowance(Some(amounts._1), Some(amounts._2))))
                )
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                //TODO: Redirect to lifetime-other-status
                Redirect(PensionsSummaryController.show(taxYear))
              }
            } else {
              Future.successful(Redirect(PensionLumpSumController.show(taxYear)))
            }
          case _ =>
            //TODO: redirect to the lifetime allowance CYA page
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }
}


