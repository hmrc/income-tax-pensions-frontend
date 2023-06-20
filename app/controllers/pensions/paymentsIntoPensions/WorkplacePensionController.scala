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

package controllers.pensions.paymentsIntoPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoPensions.routes.{PaymentsIntoPensionsCYAController, WorkplaceAmountController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoPensionPages.WorkplacePensionPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, isFinishedCheck, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.WorkplacePensionView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class WorkplacePensionController @Inject()(authAction: AuthorisedAction,
                                           pensionSessionService: PensionSessionService,
                                           errorHandler: ErrorHandler,
                                           workplacePensionView: WorkplacePensionView,
                                           formProvider: PaymentsIntoPensionFormProvider)
                                          (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      val checkRedirect = journeyCheck(WorkplacePensionPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

        val form = formProvider.workplacePensionForm(request.user.isAgent)

        data.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion match {
          case Some(value) => Future.successful(Ok(workplacePensionView(form.fill(value), taxYear)))
          case None => Future.successful(Ok(workplacePensionView(form, taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider.workplacePensionForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(workplacePensionView(formWithErrors, taxYear))),
      yesNo =>
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          val checkRedirect = journeyCheck(WorkplacePensionPage, _, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(workplacePensionPaymentsQuestion = Some(yesNo),
                totalWorkplacePensionPayments = if (yesNo) viewModel.totalWorkplacePensionPayments else None))
            }
            val redirectLocation = if (yesNo) {
              WorkplaceAmountController.show(taxYear)
            } else {
              PaymentsIntoPensionsCYAController.show(taxYear)
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
            }
          }
        }
    )
  }

}
