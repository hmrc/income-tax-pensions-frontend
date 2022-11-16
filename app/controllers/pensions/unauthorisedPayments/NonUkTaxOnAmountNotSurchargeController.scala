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
import controllers.pensions.unauthorisedPayments.routes.{NonUkTaxOnAmountNotSurchargeController, WhereAnyOfTheUnauthorisedPaymentsController}
import controllers.predicates.AuthorisedAction
import forms.{RadioButtonAmountForm, YesNoForm}
import controllers.predicates.TaxYearAction.taxYearAction
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.NonUkTaxOnAmountNotSurchargeView

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class NonUkTaxOnAmountNotSurchargeController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       nonUkTaxOnAmountNotSurchargeView: NonUkTaxOnAmountNotSurchargeView,
                                                       appConfig: AppConfig,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       clock: Clock,
                                                       ec: ExecutionContext) extends FrontendController(cc) with I18nSupport{

  def form: Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = s"unauthorisedPayments.nonUkTaxOnAmountNotSurcharge.error.noEntry",
    emptyFieldKey = s"common.pensions.error.amount.noEntry",
    wrongFormatKey = s"common.unauthorisedPayments.error.Amount.incorrectFormat",
    exceedsMaxAmountKey = s"common.pensions.error.amount.overMaximum"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async{ implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap{
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(pensionsUserData)) =>
        pensionsUserData.pensions.unauthorisedPayments.noSurchargeTaxAmount match {
            case Some(amount) => Future.successful(Ok(nonUkTaxOnAmountNotSurchargeView(form.fill((true, Some(amount))), taxYear)))
            case None =>  Future.successful(Ok(nonUkTaxOnAmountNotSurchargeView(form, taxYear)))
          }
      case Right(None) => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  def submit(taxYear : Int) : Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(optData)) =>
        optData.pensions.unauthorisedPayments.noSurchargeAmount match {
          case Some(_) =>
            form.bindFromRequest.fold(
              formWithErrors => Future.successful(BadRequest(nonUkTaxOnAmountNotSurchargeView(formWithErrors, taxYear))),
              amounts => {
                val pensionsCYAModel: PensionsCYAModel = optData.pensions
                val viewModel: UnauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments
                val updatedCyaModel: PensionsCYAModel = amounts match {
                  case (noSurchargeTaxAmountAnswer, amount) => pensionsCYAModel.copy(
                    unauthorisedPayments = viewModel.copy(
                      noSurchargeTaxAmountQuestion = Some(noSurchargeTaxAmountAnswer),
                      noSurchargeTaxAmount = if (noSurchargeTaxAmountAnswer) amount else Some(0)
                    )
                  )
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, optData.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear))
                }
              }
            )
          case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
    }
  }
}
