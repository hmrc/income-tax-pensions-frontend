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
import forms.{FormUtils, FormsProvider}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.lifetimeAllowances.PensionLumpSumDetailsView
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.lifetimeAllowances.routes.PensionLumpSumController
import models.AuthorisationRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionLumpSumDetailsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                pensionLumpSumDetailsView: PensionLumpSumDetailsView,
                                                appConfig: AppConfig,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler,
                                                formsProvider: FormsProvider,
                                                clock: Clock,
                                                ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {




  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)) async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          Future.successful(populateForm(data, taxYear))
        case _ =>
          //TODO: - Redirect to Annual Lifetime allowances cya page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  private def populateForm(data: PensionsUserData, taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Result = {
    val totalTaxOpt = data.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.amount)
    val taxPaidOpt = data.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.taxPaid)

    val form = (totalTaxOpt, taxPaidOpt) match {
      case (Some(amountBeforeTax), None) => formsProvider.pensionLumpSumAmountForm(request.user.isAgent)
        .fill((Some(amountBeforeTax), None): (Option[BigDecimal], Option[BigDecimal]))
      case (None, Some(nonUkTaxPaid)) => formsProvider.pensionLumpSumAmountForm(request.user.isAgent)
        .fill((None, Some(nonUkTaxPaid)): (Option[BigDecimal], Option[BigDecimal]))
      case (Some(amountBeforeTax), Some(nonUkTaxPaid)) => formsProvider.pensionLumpSumAmountForm(request.user.isAgent)
        .fill((Some(amountBeforeTax), Some(nonUkTaxPaid)))
      case _ => formsProvider.pensionPaymentsForm(request.user)
    }
    Ok(pensionLumpSumDetailsView(form, taxYear))
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formsProvider.pensionLumpSumAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionLumpSumDetailsView(formWithErrors, taxYear))),
      amounts => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optPensionUserData) => optPensionUserData match {
            case Some(data) =>
              if (data.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion.contains(true)) {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel: PensionLifetimeAllowancesViewModel = pensionsCYAModel.pensionLifetimeAllowances
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(
                    pensionLifetimeAllowances = viewModel.copy(
                      pensionAsLumpSum = Some(LifetimeAllowance(amounts._1, amounts._2)))
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
      }
    )
  }
}


