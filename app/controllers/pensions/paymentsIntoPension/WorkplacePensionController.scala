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
import controllers.pensions.paymentsIntoPension.routes.{PaymentsIntoPensionsCYAController, WorkplaceAmountController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.WorkplacePensionPage
import views.html.pensions.paymentsIntoPensions.WorkplacePensionView

import javax.inject.Inject
import models.redirects.ConditionalRedirect
import services.RedirectService.{PaymentsIntoPensionsRedirects, isFinishedCheck, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

class WorkplacePensionController @Inject()(authAction: AuthorisedAction,
                                           pensionSessionService: PensionSessionService,
                                           errorHandler: ErrorHandler,
                                           workplacePensionView: WorkplacePensionView,
                                           formProvider: PaymentsIntoPensionFormProvider)
                                          (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int, fromGatewayChangeLink: Boolean = false): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        val form = formProvider.workplacePensionForm(request.user.isAgent)

        data.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion match {
          case Some(value) => Future.successful(Ok(workplacePensionView(form.fill(value), taxYear, fromGatewayChangeLink)))
          case None => Future.successful(Ok(workplacePensionView(form, taxYear, fromGatewayChangeLink)))
        }
      }
    }
  }

  def submit(taxYear: Int, fromGatewayChangeLink: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider.workplacePensionForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(workplacePensionView(formWithErrors, taxYear))),
      yesNo =>
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(workplacePensionPaymentsQuestion = Some(yesNo),
                totalWorkplacePensionPayments = if (yesNo) viewModel.totalWorkplacePensionPayments else None))
            }
            val redirectLocation = if (yesNo) {
              WorkplaceAmountController.show(taxYear, fromGatewayChangeLink)
            } else {
              PaymentsIntoPensionsCYAController.show(taxYear)
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              if (!fromGatewayChangeLink && !yesNo){
                isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
              } else {
                Redirect(redirectLocation)
              }
            }
          }
        }
    )
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(WorkplacePensionPage, cya, taxYear)
  }
}
