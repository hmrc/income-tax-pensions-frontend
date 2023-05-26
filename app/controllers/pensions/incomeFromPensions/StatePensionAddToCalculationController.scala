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
import controllers.predicates.ActionsProvider
import forms.FormsProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.incomeFromPensions.StatePensionAddToCalculationView
import controllers.pensions.routes.PensionsSummaryController
import models.pension.statebenefits.StateBenefitViewModel
import models.requests.UserSessionDataRequest
import play.api.data.Form
import utils.{Clock, SessionHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionAddToCalculationController @Inject () (actionsProvider: ActionsProvider,
                                                         pensionSessionService: PensionSessionService,
                                                         view: StatePensionAddToCalculationView,
                                                         formsProvider: FormsProvider,
                                                         errorHandler: ErrorHandler)
                                                        (implicit val mcc: MessagesControllerComponents,
                                                         appConfig: AppConfig,
                                                         clock: Clock)
  extends FrontendController(mcc) with SessionHelper with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      sessionData.pensionsUserData.pensions.incomeFromPensions.statePensionLumpSum.fold {
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      } { sp: StateBenefitViewModel =>
          sp.addToCalculation.fold {
            Ok(view(formsProvider.statePensionAddToCalculationForm(sessionData.user.isAgent), taxYear))
          } {
            addToCalculation: Boolean =>
              val filledForm: Form[Boolean] = formsProvider.statePensionAddToCalculationForm(sessionData.user.isAgent)
                .fill(addToCalculation)
              Ok(view(filledForm, taxYear))
          }
        }
  }

  def submit(taxYear: Int): Action[AnyContent] = ???
}
