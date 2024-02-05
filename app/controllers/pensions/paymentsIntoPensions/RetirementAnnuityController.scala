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
import controllers.pensions.paymentsIntoPensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoPensionPages.RetirementAnnuityPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.PayIntoRetirementAnnuityContractView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RetirementAnnuityController @Inject() (authAction: AuthorisedAction,
                                             view: PayIntoRetirementAnnuityContractView,
                                             pensionSessionService: PensionSessionService,
                                             errorHandler: ErrorHandler,
                                             formProvider: PaymentsIntoPensionFormProvider,
                                             mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      val checkRedirect = journeyCheck(RetirementAnnuityPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
        val form = formProvider.retirementAnnuityForm(request.user.isAgent)
        data.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion match {
          case Some(value) => Future.successful(Ok(view(form.fill(value), taxYear)))
          case None        => Future.successful(Ok(view(form, taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider
      .retirementAnnuityForm(request.user.isAgent)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNo =>
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
            val checkRedirect = journeyCheck(RetirementAnnuityPage, _, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
              val pensionsCYAModel: PensionsCYAModel       = data.pensions
              val viewModel: PaymentsIntoPensionsViewModel = pensionsCYAModel.paymentsIntoPension
              val updatedCyaModel: PensionsCYAModel =
                pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(
                  retirementAnnuityContractPaymentsQuestion = Some(yesNo),
                  totalRetirementAnnuityContractPayments = if (yesNo) viewModel.totalRetirementAnnuityContractPayments else None
                ))
              val redirectLocation =
                if (yesNo) RetirementAnnuityAmountController.show(taxYear) else WorkplacePensionController.show(taxYear)

              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                errorHandler.internalServerError()) {
                isFinishedCheck(updatedCyaModel.paymentsIntoPension, taxYear, redirectLocation, cyaPageCall)
              }
            }
          }
      )
  }

}
