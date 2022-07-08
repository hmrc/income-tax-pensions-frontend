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
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.unauthorisedPayments.SurchargeAmountView

import javax.inject.Inject
import scala.concurrent.Future

class SurchargeAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                          authAction: AuthorisedAction,
                                          view: SurchargeAmountView,
                                          appConfig: AppConfig,
                                          pensionSessionService: PensionSessionService,
                                          errorHandler: ErrorHandler,
                                          clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {


  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "unauthorisedPayments.surchargeAmount.error.noEntry",
    wrongFormatKey = "unauthorisedPayments.surchargeAmount.error.incorrectFormat"
  )


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if(data.pensions.unauthorisedPayments.surchargeQuestion.contains(true)){

        data.pensions.unauthorisedPayments.surchargeAmount match {
          case Some(value) => Future.successful(Ok(view(
            amountForm.fill(value), taxYear)))
          case None => Future.successful(Ok(view(amountForm, taxYear)))
          }
        }
        else{
          //TODO - redirect to unauthorised payments question page once implemented
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      case None =>
        //TODO - redirect to CYA page once implemented
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))

    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            if(data.pensions.unauthorisedPayments.surchargeQuestion.contains(true)) {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel = pensionsCYAModel.unauthorisedPayments
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(unauthorisedPayments = viewModel.copy(surchargeAmount = Some(amount)))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {

                // TODO redirect to surcharge tax amount page
                Redirect(PensionsSummaryController.show(taxYear))
              }
            }else{
              //TODO - redirect to unauthorised payments question page once implemented
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
            }

          case None =>
            //TODO - redirect to CYA page once implemented
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }
}
