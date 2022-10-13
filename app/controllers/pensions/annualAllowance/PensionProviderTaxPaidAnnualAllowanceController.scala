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

package controllers.pensions.annualAllowance

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowance.routes.PensionProviderTaxPaidAnnualAllowanceController
import controllers.pensions.lifetimeAllowance.routes.PensionProviderPaidTaxController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.RadioButtonAmountForm
import models.mongo.PensionsCYAModel
import models.pension.charges.{PensionAnnualAllowancesViewModel, UnauthorisedPaymentsViewModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowance.PensionProviderTaxPaidAnnualAllowanceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionProviderTaxPaidAnnualAllowanceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                authAction: AuthorisedAction,
                                                                pensionProviderTaxPaidAnnualAllowanceView: PensionProviderTaxPaidAnnualAllowanceView,
                                                                appConfig: AppConfig,
                                                                pensionSessionService: PensionSessionService,
                                                                errorHandler: ErrorHandler,
                                                                clock: Clock,
                                                                ec: ExecutionContext) extends FrontendController(cc)  with I18nSupport {

  def form(isAgent: Boolean): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = "common.pensions.selectYesifYourPensionProvider.noEntry",
    emptyFieldKey = "pensions.pensionSchemesTaxPaidAnnualAllowance.error.amount.noEntry",
    wrongFormatKey = "pensions.pensionSchemesTaxPaidAnnualAllowance.error.amount.inCorrectFormat",
    minAmountKey = "common.error.amountNotZero",
    exceedsMaxAmountKey = s"common.pensions.error.amountMaxLimit.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async{ implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap{
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(pensionsUserData)) =>
        val pensionsAnnualAllowancesModel = pensionsUserData.pensions.pensionsAnnualAllowances
        (pensionsAnnualAllowancesModel.pensionProvidePaidAnnualAllowanceQuestion, pensionsAnnualAllowancesModel.taxPaidByPensionProvider) match {
          case (Some(true), Some(value)) =>
            Future.successful(Ok(pensionProviderTaxPaidAnnualAllowanceView(form(request.user.isAgent).fill((true, Some(value))), taxYear)))

          case (Some(false), _) =>
            Future.successful(Ok(pensionProviderTaxPaidAnnualAllowanceView(form(request.user.isAgent).fill((false, None)), taxYear)))

          case (None, _) =>
            Future.successful(Ok(pensionProviderTaxPaidAnnualAllowanceView(form(request.user.isAgent), taxYear)))

        }
      case Right(None) => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear : Int) : Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(optData)) =>
            form(request.user.isAgent).bindFromRequest.fold(
              formWithErrors => Future.successful(BadRequest(pensionProviderTaxPaidAnnualAllowanceView(formWithErrors, taxYear))),
              input => {
                val pensionsCYAModel: PensionsCYAModel = optData.pensions
                val updatedCyaModel: PensionsCYAModel = input match {
                  case (pensionProvidePaidAnnualAllowanceAnswer, amount) => pensionsCYAModel.copy(
                    pensionsAnnualAllowances = pensionsCYAModel.pensionsAnnualAllowances.copy(
                      pensionProvidePaidAnnualAllowanceQuestion = Some(pensionProvidePaidAnnualAllowanceAnswer),
                      taxPaidByPensionProvider = if (pensionProvidePaidAnnualAllowanceAnswer) amount else None
                    )
                  )
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, optData.isPriorSubmission)(errorHandler.internalServerError()) {
                  if (input._1) {
                    Redirect(PensionProviderTaxPaidAnnualAllowanceController.show(taxYear))
                  } else {
                    Redirect(PensionProviderTaxPaidAnnualAllowanceController.show(taxYear))
                  }
                }
              }
            )
    }
  }
}
