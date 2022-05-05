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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromPensions.StatePensionView
import controllers.pensions.incomeFromPensions.routes._

import javax.inject.Inject
import scala.concurrent.Future

class StatePensionController @Inject()(implicit val cc: MessagesControllerComponents,
                                       authAction: AuthorisedAction,
                                       statePensionView: StatePensionView,
                                       appConfig: AppConfig,
                                       pensionSessionService: PensionSessionService,
                                       inYearAction: InYearAction,
                                       errorHandler: ErrorHandler,
                                       clock: Clock) extends FrontendController(cc) with I18nSupport {


  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.statePension.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          data.pensions.incomeFromPensions.statePension.flatMap(_.amountPaidQuestion) match {
            case Some(value) => Future.successful(Ok(statePensionView(
              yesNoForm(request.user).fill(value), taxYear)))
            case None => Future.successful(Ok(statePensionView(yesNoForm(request.user), taxYear)))
          }
        case None =>
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      yesNoForm(request.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(statePensionView(formWithErrors, taxYear))),
        yesNo => {
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            data =>
              val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
              val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  incomeFromPensions = viewModel.copy(statePension = viewModel.statePension.map(_.copy(
                    amountPaidQuestion = Some(yesNo),
                    amount = if (yesNo) viewModel.statePension.flatMap(_.amount) else None
                  ))))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
                if (yesNo) {
                  Redirect(StatePensionAmountController.show(taxYear))
                } else {
                  Redirect(StatePensionLumpSumController.show(taxYear))
                }
              }
          }
        }
      )
    }
  }
}
