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
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.FormsProvider
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import services.redirects.UnauthorisedPaymentsPages.NonUkTaxOnNotSurchargedAmountPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.unauthorisedPayments.NonUkTaxOnAmountNotResultedInSurchargeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonUKTaxOnAmountNotResultedInSurchargeController @Inject() (
    authAction: AuthorisedAction,
    view: NonUkTaxOnAmountNotResultedInSurchargeView,
    pensionSessionService: PensionSessionService,
    formsProvider: FormsProvider,
    errorHandler: ErrorHandler)(implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(mcc)
    with SessionHelper
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(NonUkTaxOnNotSurchargedAmountPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
          val noSurchargeTaxQuestion = data.pensions.unauthorisedPayments.noSurchargeTaxAmountQuestion
          val noSurchargeTaxAmount   = data.pensions.unauthorisedPayments.noSurchargeTaxAmount
          (noSurchargeTaxQuestion, noSurchargeTaxAmount) match {
            case (Some(yesNo), amount) =>
              Future.successful(Ok(view(formsProvider.unauthorisedNonUkTaxOnNotSurchargedAmountForm(request.user).fill((yesNo, amount)), taxYear)))
            case _ =>
              Future.successful(Ok(view(formsProvider.unauthorisedNonUkTaxOnNotSurchargedAmountForm(request.user), taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(NonUkTaxOnNotSurchargedAmountPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
          formsProvider
            .unauthorisedNonUkTaxOnNotSurchargedAmountForm(request.user)
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
              yesNoAmount => {
                val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
                  unauthorisedPayments = data.pensions.unauthorisedPayments.copy(
                    noSurchargeTaxAmountQuestion = Some(yesNoAmount._1),
                    noSurchargeTaxAmount = yesNoAmount._2
                  )
                )

                pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  isFinishedCheck(
                    updatedCyaModel.unauthorisedPayments,
                    taxYear,
                    WereAnyOfTheUnauthorisedPaymentsController.show(taxYear),
                    cyaPageCall)
                }
              }
            )
        }
    }
  }
}
