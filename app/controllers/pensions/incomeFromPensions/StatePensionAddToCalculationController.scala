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
import controllers.pensions.incomeFromPensions.routes.StatePensionCYAController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.StatePensionPages.AddStatePensionToIncomeTaxCalcPage
import services.redirects.StatePensionRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.StatePensionAddToCalculationView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionAddToCalculationController @Inject()(actionsProvider: ActionsProvider,
                                                       pensionSessionService: PensionSessionService,
                                                       view: StatePensionAddToCalculationView,
                                                       formsProvider: FormsProvider,
                                                       errorHandler: ErrorHandler)
                                                      (implicit val mcc: MessagesControllerComponents,
                                                       appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with SessionHelper with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>

      val checkRedirect = journeyCheck(AddStatePensionToIncomeTaxCalcPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
        data.pensions.incomeFromPensions.statePensionLumpSum.fold {
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        } { sp: StateBenefitViewModel =>
          sp.addToCalculation.fold {
            Future.successful(Ok(view(formsProvider.statePensionAddToCalculationForm(sessionData.user.isAgent), taxYear)))
          } { addToCalculation: Boolean =>
            val filledForm: Form[Boolean] = formsProvider.statePensionAddToCalculationForm(sessionData.user.isAgent)
              .fill(addToCalculation)
            Future.successful(Ok(view(filledForm, taxYear)))
          }
        }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      val checkRedirect = journeyCheck(AddStatePensionToIncomeTaxCalcPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
        formsProvider.statePensionAddToCalculationForm(sessionData.user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          addToCalculation => updateSessionData(data, addToCalculation, taxYear)
        )
      }
  }

  private def updateSessionData[T](pensionsUserData: PensionsUserData,
                                   addToCalculation: Boolean,
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val viewModel: IncomeFromPensionsViewModel = pensionsUserData.pensions.incomeFromPensions
    val updatedViewModels: Seq[StateBenefitViewModel] = Seq(
      viewModel.statePension.getOrElse(StateBenefitViewModel()),
      viewModel.statePensionLumpSum.getOrElse(StateBenefitViewModel())
    ).map(vm => if (vm.amountPaidQuestion.getOrElse(false)) vm.copy(addToCalculation = Some(addToCalculation)) else vm)
    val updatedUserData = pensionsUserData.copy(pensions = pensionsUserData.pensions.copy(incomeFromPensions =
      viewModel.copy(statePension = Some(updatedViewModels(0)), statePensionLumpSum = Some(updatedViewModels(1)))))

    pensionSessionService.createOrUpdateSessionData(updatedUserData).map {
      case Right(_) => Redirect(StatePensionCYAController.show(taxYear))
      case _ => errorHandler.internalServerError()
    }
  }
}
