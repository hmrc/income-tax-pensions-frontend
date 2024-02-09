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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.pensions.unauthorisedPayments.routes.{NoSurchargeAmountController, SurchargeAmountController, UnauthorisedPaymentsCYAController}
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.UnAuthorisedPaymentsForm
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData.populateSessionFromPrior
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.isFinishedCheck
import services.redirects.UnauthorisedPaymentsRedirects.cyaPageCall
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPaymentsController @Inject() (mcc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler,
                                                view: UnauthorisedPaymentsView)(implicit appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) =>
        optPensionUserData match {
          case Some(data) =>
            val surchargeQuestion: Option[Boolean]   = data.pensions.unauthorisedPayments.surchargeQuestion
            val noSurchargeQuestion: Option[Boolean] = data.pensions.unauthorisedPayments.noSurchargeQuestion
            val noQuestion: Option[Boolean]          = data.pensions.unauthorisedPayments.unauthorisedPaymentQuestion.map(!_)
            val form                                 = UnAuthorisedPaymentsForm.unAuthorisedPaymentsTypeForm(request.user)
            Future.successful(Ok(view(form, taxYear, surchargeQuestion, noSurchargeQuestion, noQuestion)))
          case None =>
            val form = UnAuthorisedPaymentsForm.unAuthorisedPaymentsTypeForm(request.user)
            Future.successful(Ok(view(form, taxYear, None, None, None)))
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    def saveDataAndRedirect(pensionsCYAModel: PensionsCYAModel, isPriorSubmission: Boolean): Future[Result] =
      UnAuthorisedPaymentsForm
        .unAuthorisedPaymentsTypeForm(request.user)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          unauthorisedPaymentsSelection => {
            val unauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments

            val updatedCyaModel: PensionsCYAModel =
              pensionsCYAModel.copy(unauthorisedPayments = unauthorisedPaymentsViewModel.copyWithQuestionsApplied(
                surchargeQuestion = Some(unauthorisedPaymentsSelection.containsYesSurcharge),
                noSurchargeQuestion = Some(unauthorisedPaymentsSelection.containsYesNotSurcharge)
              ))
            val redirectLocation =
              if (unauthorisedPaymentsSelection.containsYesSurcharge) {
                SurchargeAmountController.show(taxYear)
              } else if (unauthorisedPaymentsSelection.containsYesNotSurcharge) {
                NoSurchargeAmountController.show(taxYear)
              } else {
                UnauthorisedPaymentsCYAController.show(taxYear)
              }

            pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, isPriorSubmission)(
              errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel.unauthorisedPayments, taxYear, redirectLocation, cyaPageCall)
            }
          }
        )

    pensionSessionService.loadDataAndHandle(taxYear, request.user) { (optSessionData, optPriorData) =>
      (optSessionData, optPriorData) match {
        case (Some(sessionData), _) =>
          saveDataAndRedirect(sessionData.pensions, isPriorSubmission = false)
        case (None, Some(priorData)) =>
          val cya: PensionsCYAModel = populateSessionFromPrior(priorData)
          saveDataAndRedirect(cya, isPriorSubmission = true)
        case (None, None) =>
          val cya = PensionsCYAModel.emptyModels.copy(PaymentsIntoPensionsViewModel())
          saveDataAndRedirect(cya, isPriorSubmission = false)
      }
    }
  }
}
