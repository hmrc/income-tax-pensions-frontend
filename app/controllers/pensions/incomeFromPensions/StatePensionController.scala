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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{ActionsProvider, InYearAction}
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.StatePensionView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StatePensionController @Inject()(actionsProvider: ActionsProvider,
                                       pensionSessionService: PensionSessionService,
                                       view: StatePensionView,
                                       formsProvider: FormsProvider,
                                       inYearAction: InYearAction,
                                       errorHandler: ErrorHandler)
                                      (implicit val mcc: MessagesControllerComponents,
                                       appConfig: AppConfig,
                                       clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      val maybeYesNo: Option[Boolean] =
        sessionData.pensionsUserData.pensions.incomeFromPensions.statePension.flatMap(sP => sP.amountPaidQuestion)
      val maybeAmount: Option[BigDecimal] =
        sessionData.pensionsUserData.pensions.incomeFromPensions.statePension.flatMap(sP => sP.amount)
      (maybeYesNo, maybeAmount) match {
        case (Some(yesNo), amount) =>
          Future.successful(Ok(view(formsProvider.statePensionForm(sessionData.user).fill((yesNo, amount)), taxYear)))
        case _ =>
          Future.successful(Ok(view(formsProvider.statePensionForm(sessionData.user), taxYear)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
    inYearAction.notInYear(taxYear) {
      formsProvider.statePensionForm(sessionData.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNoAmount => {
          (yesNoAmount._1, yesNoAmount._2) match {
            case (true, amount) => updateSessionData(sessionData.pensionsUserData, yesNo = true, amount, taxYear)
            case (false, _) => updateSessionData(sessionData.pensionsUserData, yesNo = false, None, taxYear)
          }
        }
      )
    }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val viewModel: IncomeFromPensionsViewModel = pensionUserData.pensions.incomeFromPensions
    val updateStatePension: StateBenefitViewModel = viewModel.statePension match {
      case Some(value) => value.copy(amountPaidQuestion = Some(yesNo), amount = if (yesNo) amount else None)
      case _ => StateBenefitViewModel(amountPaidQuestion = Some(yesNo), amount = if (yesNo) amount else None)
    }
    val updatedCyaModel: PensionsCYAModel =
      pensionUserData.pensions.copy(incomeFromPensions = viewModel.copy(statePension = Some(updateStatePension)))
    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      if (yesNo) {
        Redirect(controllers.pensions.incomeFromPensions.routes.StatePensionStartDateController.show(taxYear))
      } else {
        Redirect(controllers.pensions.incomeFromPensions.routes.StatePensionLumpSumController.show(taxYear))
        //todo redirect to Check your State Pension or Lump sum page
      }
    }
  }
}
