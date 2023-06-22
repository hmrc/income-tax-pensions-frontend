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
import controllers.pensions.lifetimeAllowances.routes.LifeTimeAllowanceAnotherWayController
import controllers.pensions.lifetimeAllowances.routes.PensionTakenAnotherWayAmountController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{FormUtils, FormsProvider}
import models.mongo.PensionsCYAModel
import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.lifetimeAllowances.PensionTakenAnotherWayAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PensionTakenAnotherWayAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       pensionTakenAnotherWayAmountView: PensionTakenAnotherWayAmountView,
                                                       appConfig: AppConfig,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       formsProvider: FormsProvider,
                                                       clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        val totalTaxOpt = data.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.amount)
        val taxPaidOpt = data.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.taxPaid)
        (totalTaxOpt, taxPaidOpt) match {
          case (Some(totalTax), Some(taxPaid)) =>
            Future.successful(Ok(pensionTakenAnotherWayAmountView(formsProvider.pensionTakenAnotherWayAmountForm(request.user.isAgent)
              .fill((Some(totalTax), Some(taxPaid))), taxYear)))
          case (Some(totalTax), None) =>
            Future.successful(Ok(pensionTakenAnotherWayAmountView(formsProvider.pensionTakenAnotherWayAmountForm(request.user.isAgent)
              .fill((Some(totalTax), None)), taxYear)))
          case (None, Some(taxPaid)) =>
            Future.successful(Ok(pensionTakenAnotherWayAmountView(formsProvider.pensionTakenAnotherWayAmountForm(request.user.isAgent)
              .fill((None, Some(taxPaid))), taxYear)))
          case (_, _) =>
            Future.successful(Ok(pensionTakenAnotherWayAmountView(formsProvider.pensionTakenAnotherWayAmountForm(request.user.isAgent), taxYear)))
        }
      case _ =>
        //TODO: - Redirect to Annual Lifetime allowances cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))

    }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formsProvider.pensionTakenAnotherWayAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionTakenAnotherWayAmountView(formWithErrors, taxYear))),
      amounts => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(Some(optData)) =>
            if (optData.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion.contains(true)) {
              val pensionsCYAModel: PensionsCYAModel = optData.pensions
              val viewModel: PensionLifetimeAllowancesViewModel = pensionsCYAModel.pensionLifetimeAllowances
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy( pensionLifetimeAllowances = viewModel.copy(
                    pensionPaidAnotherWay = if (amounts._1.isEmpty && amounts._2.isEmpty) None else Some(LifetimeAllowance(amounts._1, amounts._2))
                ))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, optData.isPriorSubmission)(errorHandler.internalServerError()) {
                //TODO: (next page - Redirect to pension scheme that paid page)
                Redirect(PensionTakenAnotherWayAmountController.show(taxYear))
              }
            } else {
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
            }
          case _ =>
            //TODO: redirect to the lifetime allowance CYA page
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }


}
