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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.TaxEmployerPaymentsPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.paymentsIntoOverseasPensions.TaxEmployerPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEmployerPaymentsController @Inject() (authAction: AuthorisedAction,
                                               view: TaxEmployerPaymentsView,
                                               pensionSessionService: PensionSessionService,
                                               errorHandler: ErrorHandler,
                                               mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"overseasPension.taxEmployerPayments.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) =>
        val checkRedirect = journeyCheck(TaxEmployerPaymentsPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optPensionUserData, cyaPageCall(taxYear))(checkRedirect) { data: PensionsUserData =>
          data.pensions.paymentsIntoOverseasPensions.taxPaidOnEmployerPaymentsQuestion match {
            case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
            case None        => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNo =>
          pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
            case Right(Some(data)) =>
              val cyaModel: PensionsCYAModel = data.pensions
              val updatedViewModel: PaymentsIntoOverseasPensionsViewModel =
                if (yesNo) {
                  cyaModel.paymentsIntoOverseasPensions.copy(taxPaidOnEmployerPaymentsQuestion = Some(true), reliefs = Seq.empty)
                } else {
                  cyaModel.paymentsIntoOverseasPensions.copy(taxPaidOnEmployerPaymentsQuestion = Some(false))
                }
              val updatedCyaModel: PensionsCYAModel = cyaModel.copy(paymentsIntoOverseasPensions = updatedViewModel)

              val redirectLocation =
                if (yesNo) cyaPageCall(taxYear) else redirectForSchemeLoop(updatedCyaModel.paymentsIntoOverseasPensions.reliefs, taxYear)

              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                errorHandler.internalServerError()) {
                isFinishedCheck(updatedCyaModel.paymentsIntoOverseasPensions, taxYear, redirectLocation, cyaPageCall)
              }
            case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
          }
      )
  }
}
