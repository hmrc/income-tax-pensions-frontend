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
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import controllers.pensions.routes.PensionsSummaryController
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.WhereAnyOfTheUnauthorisedPaymentsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhereAnyOfTheUnauthorisedPaymentsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                            authAction: AuthorisedAction,
                                                            whereAnyOfTheUnauthorisedPaymentsView: WhereAnyOfTheUnauthorisedPaymentsView,
                                                            appConfig: AppConfig,
                                                            pensionSessionService: PensionSessionService,
                                                            errorHandler: ErrorHandler,
                                                            clock: Clock) extends FrontendController(cc) with I18nSupport {


  def yesNoForm(): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"common.unauthorisedPayments.error.checkbox.or.radioButton.noEntry"
  )


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optPensionUserData) => optPensionUserData match {
            case Some(data) =>
              data.pensions.unauthorisedPayments.ukPensionSchemesQuestion match {
                case Some(value) => Future.successful(Ok(whereAnyOfTheUnauthorisedPaymentsView(
                  yesNoForm().fill(value), taxYear)))
                case None => Future.successful(Ok(whereAnyOfTheUnauthorisedPaymentsView(yesNoForm(), taxYear)))
              }
            case None =>
              //TODO - redirect to CYA page once implemented
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(whereAnyOfTheUnauthorisedPaymentsView(formWithErrors, taxYear))),
      yesNo => {

        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optPensionUserData) => optPensionUserData match {
            case Some(data) =>
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: UnauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  unauthorisedPayments = viewModel.copy(ukPensionSchemesQuestion = Some(yesNo)))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                if (yesNo) {
                  //TODO - redirect to "Pension Scheme Tax Reference PSTR" page once implemented
                  Redirect(PensionsSummaryController.show(taxYear))
                } else {
                  //TODO - redirect to "Check your unauthorised payments page" page once implemented
                  Redirect(PensionsSummaryController.show(taxYear))
                }
              }
            case _ => {
              //TODO - redirect to CYA page once implemented
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
            }
          }
        }
      }
    )
  }
}
