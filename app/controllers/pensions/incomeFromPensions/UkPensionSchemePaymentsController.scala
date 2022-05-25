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
import controllers.pensions.incomeFromPensions.routes.PensionSchemeDetailsController
import controllers.predicates.TaxYearAction.taxYearAction
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
import views.html.pensions.incomeFromPensions.UkPensionSchemePaymentsView

import javax.inject.Inject
import scala.concurrent.Future

class UkPensionSchemePaymentsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                              appConfig: AppConfig,
                                              authAction: AuthorisedAction,
                                              inYearAction: InYearAction,
                                              pensionSessionService: PensionSessionService,
                                              errorHandler: ErrorHandler,
                                              view: UkPensionSchemePaymentsView,
                                              clock: Clock) extends FrontendController(mcc) with I18nSupport {

  private def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.ukPensionSchemePayments.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    inYearAction.notInYear(taxYear) {
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          data.pensions.incomeFromPensions.uKPensionIncomesQuestion match {
            case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
            case _ => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
          }
        case _ =>
          //TODO - redirect to CYA page once implemented
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
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
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  incomeFromPensions = viewModel.copy(uKPensionIncomesQuestion = Some(yesNo),
                    uKPensionIncomes = if (yesNo) viewModel.uKPensionIncomes else Seq.empty))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                if (yesNo) {
                  Redirect(PensionSchemeDetailsController.show(taxYear, None))
                } else {
                  //TODO redirect to Pension CYA page
                  Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
                }
              }
            case _ =>
              //TODO redirect to Pension CYA page
              Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
          }
        }
      )
    }
  }

}
