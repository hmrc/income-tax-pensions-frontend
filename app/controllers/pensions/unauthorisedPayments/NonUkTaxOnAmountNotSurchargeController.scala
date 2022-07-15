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
import controllers.pensions.unauthorisedPayments.routes.NonUkTaxOnAmountNotSurchargeController
import controllers.predicates.AuthorisedAction
import forms.{RadioButtonAmountForm, YesNoForm}
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.{PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import play.api.data
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.NonUkTaxOnAmountNotSurchargeView

import javax.inject.Inject
import scala.concurrent.Future

class NonUkTaxOnAmountNotSurchargeController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       nonUkTaxOnAmountNotSurchargeView: NonUkTaxOnAmountNotSurchargeView,
                                                       appConfig: AppConfig,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport{

  def form: Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = s"unauthorisedPayments.didYouPayNonUkTax.error.noEntry",
    emptyFieldKey = s"unauthorisedPayments.didYouPayNonUkTax.error.Amount.noEntry",
    wrongFormatKey = s"unauthorisedPayments.didYouPayNonUkTax.error.Amount.incorrectFormat",
    exceedsMaxAmountKey = s"unauthorisedPayments.didYouPayNonUkTax.error.Amount.overMaximum"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request => {
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) if (data.pensions.unauthorisedPayments.noSurchargeAmount.isDefined) =>
      data.pensions.unauthorisedPayments.noSurchargeTaxAmount match {
        case Some(value) if value == 0 =>
          Future.successful(Ok(
            nonUkTaxOnAmountNotSurchargeView(form.fill((false, Some(value))), taxYear)))

        case Some(value) =>
          Future.successful(Ok(nonUkTaxOnAmountNotSurchargeView(form.fill((true, Some(value))), taxYear)))

        case None =>
          Future.successful(Ok(nonUkTaxOnAmountNotSurchargeView(form, taxYear)))
      }
      case Some(data) if (data.pensions.unauthorisedPayments.noSurchargeAmount.isEmpty) =>
        //todo redirect to unauthorised cya page
        Future.successful(Ok(nonUkTaxOnAmountNotSurchargeView(form, taxYear)))
      case _ =>
        //todo - redirect to unauthorised cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request => {
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.unauthorisedPayments.noSurchargeAmount.isDefined) {
          form.bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(nonUkTaxOnAmountNotSurchargeView(formWithErrors, taxYear))),
            amounts => {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: UnauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  unauthorisedPayments = viewModel.copy(
                    noSurchargeTaxAmountQuestion = Some(amounts._1),
                    noSurchargeTaxAmount = if (amounts._1) amounts._2 else Some(0)
                  )
                )
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                //TODO: next page (were any of the unauthorised payments from a uk pension scheme page)
                Redirect(NonUkTaxOnAmountNotSurchargeController.show(taxYear))
              }
            }
          )
        } else {
          //todo redirect to unauthorised cya page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      case _ =>
        //TODO: redirect to unauthorised cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  }



}
