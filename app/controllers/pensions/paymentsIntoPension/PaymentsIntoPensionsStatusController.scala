/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.paymentsIntoPension

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.TailoringEnabledFilterAction.tailoringEnabledFilterAction
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsStatusView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsIntoPensionsStatusController @Inject()(authAction: AuthorisedAction,
                                                     formProvider: PaymentsIntoPensionFormProvider,
                                                     view: PaymentsIntoPensionsStatusView,
                                                     pensionSessionService: PensionSessionService,
                                                     errorHandler: ErrorHandler)
                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen tailoringEnabledFilterAction(taxYear) andThen taxYearAction(taxYear)).async {
    implicit request =>
      Future.successful(
        Ok(view(taxYear, formProvider.paymentsIntoPensionsStatusForm(request.user.isAgent)))
      )
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen tailoringEnabledFilterAction(taxYear) andThen taxYearAction(taxYear)).async { implicit request =>
    formProvider.paymentsIntoPensionsStatusForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors))),
      yesNoAnswer =>
        if (yesNoAnswer) {
          Future(Redirect(PensionsSummaryController.show(taxYear)))
        } else {
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
    )
  }
}
