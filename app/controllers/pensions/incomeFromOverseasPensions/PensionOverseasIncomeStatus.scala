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

package controllers.pensions.incomeFromOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsView
import controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus
import controllers.pensions.routes.PensionsSummaryController


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionOverseasIncomeStatus @Inject()(authAction: AuthorisedAction,
                                            incomeFromOverseasPensionsView: IncomeFromOverseasPensionsView,
                                            pensionSessionService: PensionSessionService,
                                            errorHandler: ErrorHandler
                                           )(implicit val mcc: MessagesControllerComponents,
                                             appConfig: AppConfig,
                                             clock: Clock,
                                             ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"incomeFromOverseasPensions.incomeFromOverseasPension.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          val form =
            data.pensions.incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.fold(yesNoForm(request.user)
            )(yesNoForm(request.user).fill(_))
          Future.successful(Ok(incomeFromOverseasPensionsView(form, taxYear)))
        case None =>
          //TODO - redirect to CYA page once implemented
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(incomeFromOverseasPensionsView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) =>
            val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
              incomeFromOverseasPensionsViewModel = data.pensions.incomeFromOverseasPensionsViewModel.copy(
                paymentsFromOverseasPensionsQuestion = Some(yesNo)))
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              if (yesNo) {
                Redirect(PensionOverseasIncomeStatus.show(taxYear)) //TODO - redirect to SASS-2587
              } else {
                Redirect(PensionOverseasIncomeStatus.show(taxYear)) //TODO - redirect to SASS-2588
              }
            }
          case _ =>
            //TODO - redirect to CYA page once implemented
            Future.successful(Redirect(PensionOverseasIncomeStatus.show(taxYear)))
        }
      }
    )
  }
}