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
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.{Inject, Singleton}
import models.User
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.StateBenefitViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromPensions.StatePensionLumpSumView

import scala.concurrent.Future

@Singleton
class StatePensionLumpSumController @Inject()(implicit val mcc: MessagesControllerComponents,
                                              appConfig: AppConfig,
                                              authAction: AuthorisedAction,
                                              pensionSessionService: PensionSessionService,
                                              errorHandler: ErrorHandler,
                                              view: StatePensionLumpSumView,
                                              clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.amountPaidQuestion) match {
          case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
          case _ => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
      }
      case _ =>
        //TODO - redirect to CYA page once implemented
        Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo =>
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel = pensionsCYAModel.incomeFromPensions

            val updatedBenefitModel: StateBenefitViewModel =
              if(yesNo){
                viewModel.statePensionLumpSum.fold(StateBenefitViewModel().copy(amountPaidQuestion = Some(yesNo)))(
                  _.copy(amountPaidQuestion = Some(yesNo)))
              } else {
                viewModel.statePensionLumpSum.fold(StateBenefitViewModel().copy(amountPaidQuestion = Some(yesNo)))(
                  _.copy(amountPaidQuestion = Some(yesNo), amount = None))
              }

            val updatedCyaModel =
              pensionsCYAModel.copy(incomeFromPensions = viewModel.copy(statePensionLumpSum = Some(updatedBenefitModel)))

            pensionSessionService.createOrUpdateSessionData(
              request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              if(yesNo) {
                //TODO - redirect to 'How much was your State Pension lump sum' page
                Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
              } else {
                //TODO - redirect to 'Did you pay tax on the State Pension lump sum' page
                Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
              }
            }
        }
    )
  }

  private def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"incomeFromPensions.statePensionLumpSum.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )
}
