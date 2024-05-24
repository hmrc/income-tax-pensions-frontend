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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoOverseasPensions.routes.{PaymentsIntoOverseasPensionsCYAController, TaxEmployerPaymentsController}
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.EmployerPayOverseasPensionPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.paymentsIntoOverseasPensions.EmployerPayOverseasPensionView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmployerPayOverseasPensionController @Inject() (authAction: AuthorisedAction,
                                                      view: EmployerPayOverseasPensionView,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) =>
        val checkRedirect = journeyCheck(EmployerPayOverseasPensionPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optPensionUserData, cyaPageCall(taxYear))(checkRedirect) { data =>
          data.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion match {
            case Some(value) =>
              Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
            case None =>
              Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
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
            case Right(optPensionUserData) =>
              val checkRedirect = journeyCheck(EmployerPayOverseasPensionPage, _: PensionsCYAModel, taxYear)
              redirectBasedOnCurrentAnswers(taxYear, optPensionUserData, cyaPageCall(taxYear))(checkRedirect) { data: PensionsUserData =>
                val cyaModel: PensionsCYAModel = data.pensions
                val updatedViewModel: PaymentsIntoOverseasPensionsViewModel =
                  if (yesNo) {
                    cyaModel.paymentsIntoOverseasPensions.copy(employerPaymentsQuestion = Some(true))
                  } else {
                    cyaModel.paymentsIntoOverseasPensions.copy(
                      employerPaymentsQuestion = Some(false),
                      taxPaidOnEmployerPaymentsQuestion = None,
                      schemes = List.empty
                    )
                  }
                val updatedCyaModel: PensionsCYAModel = cyaModel.copy(paymentsIntoOverseasPensions = updatedViewModel)

                val redirectLocation =
                  if (yesNo) TaxEmployerPaymentsController.show(taxYear) else PaymentsIntoOverseasPensionsCYAController.show(TaxYear(taxYear))

                pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  isFinishedCheck(updatedCyaModel.paymentsIntoOverseasPensions, taxYear, redirectLocation, cyaPageCall)
                }
              }
            case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))

          }
      )
  }

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"overseasPension.employerPayOverseasPension.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

}
