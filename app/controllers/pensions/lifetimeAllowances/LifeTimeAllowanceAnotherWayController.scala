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

package controllers.pensions.lifetimeAllowances

import config.{AppConfig, ErrorHandler}
import controllers.pensions.lifetimeAllowances.routes.{LifetimeAllowanceCYAController, PensionTakenAnotherWayAmountController}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.LifetimeAllowancesPages.LifetimeAllowanceAnotherWayPage
import services.redirects.LifetimeAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.lifetimeAllowances.LifeTimeAllowanceAnotherWayView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LifeTimeAllowanceAnotherWayController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      view: LifeTimeAllowanceAnotherWayView,
                                                      appConfig: AppConfig,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      clock: Clock, ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"lifetimeAllowance.lifetimeAllowanceAnotherWay.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>

          val checkRedirect = journeyCheck(LifetimeAllowanceAnotherWayPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
            data =>
              data.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion match {
                case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
                case None => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
              }
          }
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo =>
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optPensionUserData) => optPensionUserData match {
            case Some(data) =>

              val checkRedirect = journeyCheck(LifetimeAllowanceAnotherWayPage, _: PensionsCYAModel, taxYear)
              redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
                data =>
                  val pensionsCYAModel = data.pensions
                  val viewModel = pensionsCYAModel.pensionLifetimeAllowances
                  val updatedCyaModel = pensionsCYAModel.copy(pensionLifetimeAllowances = viewModel.copy(
                    pensionPaidAnotherWayQuestion = Some(yesNo),
                    pensionPaidAnotherWay = if (yesNo) viewModel.pensionPaidAnotherWay else None,
                    pensionSchemeTaxReferences = if (yesNo) viewModel.pensionSchemeTaxReferences else None))

                  val redirectLocation = if (yesNo) PensionTakenAnotherWayAmountController.show(taxYear) else LifetimeAllowanceCYAController.show(taxYear)
                  pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                    errorHandler.internalServerError()) {

                    isFinishedCheck(updatedCyaModel.pensionLifetimeAllowances, taxYear, redirectLocation, cyaPageCall)
                  }
              }
            case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        }
    )
  }

}
