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
import controllers.pensions.annualAllowances.routes.{AnnualAllowanceCYAController, ReducedAnnualAllowanceTypeController}
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.AnnualAllowancesPages.ReducedAnnualAllowancePage
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.annualAllowances.ReducedAnnualAllowanceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReducedAnnualAllowanceController @Inject() (cc: MessagesControllerComponents,
                                                  actionsProvider: ActionsProvider,
                                                  view: ReducedAnnualAllowanceView,
                                                  pensionSessionService: PensionSessionService,
                                                  formsProvider: FormsProvider,
                                                  errorHandler: ErrorHandler)(implicit appConfig: AppConfig)
    extends FrontendController(cc)
    with I18nSupport {
  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(ReducedAnnualAllowancePage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val yesNoForm = formsProvider.reducedAnnualAllowanceForm(request.user)
      data.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
        case Some(question) => Future.successful(Ok(view(yesNoForm.fill(question), taxYear)))
        case None           => Future.successful(Ok(view(yesNoForm, taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(ReducedAnnualAllowancePage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      formsProvider
        .reducedAnnualAllowanceForm(request.user)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          yesNo => updateSessionData(data, yesNo, taxYear)
        )
    }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, reducedAnnualAllowanceQ: Boolean, taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {

    val pensionsCYAModel: PensionsCYAModel          = pensionUserData.pensions
    val viewModel: PensionAnnualAllowancesViewModel = pensionsCYAModel.pensionsAnnualAllowances
    val updatedCyaModel: PensionsCYAModel = pensionsCYAModel.copy(
      pensionsAnnualAllowances = if (reducedAnnualAllowanceQ) {
        viewModel.copy(reducedAnnualAllowanceQuestion = Some(true))
      } else {
        PensionAnnualAllowancesViewModel(reducedAnnualAllowanceQuestion = Some(false))
      }
    )
    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(
        if (reducedAnnualAllowanceQ) {
          ReducedAnnualAllowanceTypeController.show(taxYear)
        } else {
          AnnualAllowanceCYAController.show(taxYear)
        }
      )
    }
  }

}
