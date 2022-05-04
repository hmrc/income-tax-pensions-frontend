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
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.incomeFromPensions.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.StatePensionLumpSumAmountView

import javax.inject.Inject
import scala.concurrent.Future

class StatePensionLumpSumAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                    authAction: AuthorisedAction,
                                                    statePensionLumpSumAmountView: StatePensionLumpSumAmountView,
                                                    appConfig: AppConfig,
                                                    pensionSessionService: PensionSessionService,
                                                    inYearAction: InYearAction,
                                                    errorHandler: ErrorHandler,
                                                    clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {


  def amountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"pensions.statePensionLumpSum.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"pensions.statePensionLumpSum.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"pensions.statePensionLumpSum.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          if (data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.amountPaidQuestion).contains(true)) {
            data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.amount) match {
              case Some(amount) =>
                Future.successful(Ok(statePensionLumpSumAmountView(amountForm(request.user.isAgent).fill(amount), taxYear)))
              case None => Future.successful(Ok(statePensionLumpSumAmountView(amountForm(request.user.isAgent), taxYear)))
            }
          } else {
            Future.successful(Redirect(StatePensionLumpSumController.show(taxYear)))
          }
        case _ =>
          //todo - cya page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }

  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      amountForm(request.user.isAgent).bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(statePensionLumpSumAmountView(formWithErrors, taxYear))),
        amount => {
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            case Some(data) =>
              if (data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.amountPaidQuestion).contains(true)) {

                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions
                val statePensionLumpSumModel: Option[StateBenefitViewModel] = viewModel.statePensionLumpSum
                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(statePensionLumpSum = statePensionLumpSumModel.map(_.copy(amount = Some(amount)))))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(TaxPaidOnStatePensionLumpSumController.show(taxYear))
                }

              } else {
                Future.successful(Redirect(StatePensionLumpSumController.show(taxYear)))
              }

            case _ =>
              //TODO: redirect to the income from pensions CYA page
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }

        }
      )
    }
  }

}
