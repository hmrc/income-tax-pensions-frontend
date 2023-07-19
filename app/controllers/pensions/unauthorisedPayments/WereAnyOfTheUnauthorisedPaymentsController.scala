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
import controllers.pensions.unauthorisedPayments.routes._
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.WereAnyUnauthPaymentsFromUkPensionSchemePage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.WereAnyOfTheUnauthorisedPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WereAnyOfTheUnauthorisedPaymentsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                           authAction: AuthorisedAction,
                                                           view: WereAnyOfTheUnauthorisedPaymentsView,
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
      case Right(optData) =>
        val checkRedirect = journeyCheck(WereAnyUnauthPaymentsFromUkPensionSchemePage, _, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

          data.pensions.unauthorisedPayments.ukPensionSchemesQuestion match {
            case Some(value) => Future.successful(Ok(view(yesNoForm().fill(value), taxYear)))
            case None => Future.successful(Ok(view(yesNoForm(), taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optData) =>
            val checkRedirect = journeyCheck(WereAnyUnauthPaymentsFromUkPensionSchemePage, _, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: UnauthorisedPaymentsViewModel = pensionsCYAModel.unauthorisedPayments
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(unauthorisedPayments =
                  if (yesNo) viewModel.copy(ukPensionSchemesQuestion = Some(true))
                  else viewModel.copy(ukPensionSchemesQuestion = Some(false), pensionSchemeTaxReference = None))
              }

              pensionSessionService.createOrUpdateSessionData(
                request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {

                if (yesNo) Redirect(redirectForSchemeLoop(schemes = updatedCyaModel.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq()), taxYear))
                else Redirect(UnauthorisedPaymentsCYAController.show(taxYear))
              }
            }
        }
      }
    )
  }
}
