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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes._
import controllers.pensions.unauthorisedPayments.routes.{NonUkTaxOnAmountNotSurchargeController, SurchargeAmountController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.UnAuthorisedPaymentsForm
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.UnauthorisedPaymentsView
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UnAuthorisedPaymentsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                               appConfig: AppConfig,
                                               authAction: AuthorisedAction,
                                               pensionSessionService: PensionSessionService,
                                               errorHandler: ErrorHandler,
                                               view: UnauthorisedPaymentsView,
                                               clock: Clock) extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          val surchargeQuestion = data.pensions.unauthorisedPayments.surchargeQuestion
          val noSurchargeQuestion = data.pensions.unauthorisedPayments.noSurchargeQuestion
          var noQuestion: Option[Boolean] = Some(false)

          def setNoQuestion = {
            surchargeQuestion match {
              case Some(true) => noQuestion = Some(false)
              case Some(false) => noQuestion = data.pensions.unauthorisedPayments.noValueQuestion
              case None => noQuestion = data.pensions.unauthorisedPayments.noValueQuestion
                noSurchargeQuestion match {
                  case Some(true) => noQuestion = Some(false)
                  case Some(false) => noQuestion = data.pensions.unauthorisedPayments.noValueQuestion
                  case None => noQuestion = data.pensions.unauthorisedPayments.noValueQuestion
                }
            }
          }
          setNoQuestion
          val form = UnAuthorisedPaymentsForm.unAuthorisedPaymentsTypeForm()
            Future.successful(Ok(view(form, taxYear, surchargeQuestion, noSurchargeQuestion, noQuestion)))
        case None =>
          //TODO - redirect to CYA page once implemented
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
            UnAuthorisedPaymentsForm.unAuthorisedPaymentsTypeForm().bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
              unauthorisedPaymentsSelection => {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val unauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments

                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(unauthorisedPayments = unauthorisedPaymentsViewModel.copy(
                    surchargeQuestion = Some(unauthorisedPaymentsSelection.containsYesSurcharge),
                    noSurchargeQuestion = Some(unauthorisedPaymentsSelection.containsYesNotSurcharge),
                    noValueQuestion = Some(unauthorisedPaymentsSelection.containsNoVal)
                  ))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {

                  if (unauthorisedPaymentsSelection.containsYesSurcharge) {
                    Redirect(SurchargeAmountController.show(taxYear))
                  }
                  else if (unauthorisedPaymentsSelection.containsYesNotSurcharge) {
                    //TODO - redirect to unauthorised payments that did not result in a surcharge page once implemented
                    Redirect(NonUkTaxOnAmountNotSurchargeController.show(taxYear))
                  } else {
                    //TODO - redirect to Check your unauthorised payments page once implemented
                    Redirect(PensionsSummaryController.show(taxYear))
                  }
                }
              }
            )
        case None =>
            //TODO - redirect to Check your unauthorised payments page once implemented
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }
}