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
import controllers.pensions.incomeFromPensions.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromPensions.TaxPaidOnStatePensionLumpSumView
import controllers.pensions.incomeFromPensions.routes.TaxPaidOnLumpSumAmountController
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}

import javax.inject.Inject
import scala.concurrent.Future

class TaxPaidOnStatePensionLumpSumController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       inYearAction: InYearAction,
                                                       view: TaxPaidOnStatePensionLumpSumView,
                                                       appConfig: AppConfig,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport {


  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.taxPaidOnStatePensionLumpSum.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>

          data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaidQuestion) match {
            case Some(taxPaidQuestion) => Future.successful(Ok(view(yesNoForm(request.user).fill(taxPaidQuestion), taxYear)))
            case _ => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
          }

        //TODO: redirect to the income from pensions CYA page
        case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      yesNoForm(request.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNo => {
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            case Some(data) =>

              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions

              val updatedStatePensionLumpSum: StateBenefitViewModel = viewModel.statePensionLumpSum match {
                case Some(lumpSum) => lumpSum.copy(taxPaidQuestion = Some(yesNo), taxPaid = if (yesNo) lumpSum.taxPaid else None)
                case _ => StateBenefitViewModel(taxPaidQuestion = Some(yesNo))
              }

              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(statePensionLumpSum = Some(updatedStatePensionLumpSum)))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                if (yesNo) {
                  Redirect(TaxPaidOnLumpSumAmountController.show(taxYear))
                } else {
                  //TODO: redirect to the next page - income from pensions CYA page or other UK income?
                  // not sure from prototype but next section is not ready yet
                  Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
                }
              }

            //TODO: redirect to the income from pensions CYA page
            case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))

          }
        }
      )
    }
  }
}
