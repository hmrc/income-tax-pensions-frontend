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
import controllers.predicates.AuthorisedAction
import forms.{AmountForm, FormUtils, No}
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.annualAllowance.TaxPaidByPensionProviderAmountView
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowance.routes._

import javax.inject.Inject
import scala.concurrent.Future

class TaxPaidByPensionProviderAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                         authAction: AuthorisedAction,
                                                         view: TaxPaidByPensionProviderAmountView,
                                                         appConfig: AppConfig,
                                                         pensionSessionService: PensionSessionService,
                                                         errorHandler: ErrorHandler,
                                                         clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"pensions.annualAllowanceTaxPaidByPensionProviderAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"pensions.annualAllowanceTaxPaidByPensionProviderAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"pensions.annualAllowanceTaxPaidByPensionProviderAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        if (data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion.isDefined
          && !data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion.contains(No.toString)) {
          data.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider match {
            case Some(amount) =>
              Future.successful(Ok(view(amountForm(request.user.isAgent).fill(amount), taxYear)))
            case None => Future.successful(Ok(view(amountForm(request.user.isAgent), taxYear)))
          }
        } else {
          Future.successful(Redirect(PensionProviderPaidTaxController.show(taxYear)))
        }
      case _ =>
        //TODO: redirect to the annual allowances CYA page when available
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm(request.user.isAgent).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          case Some(data) =>
            if (data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion.isDefined
              && !data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion.contains(No.toString)) {

              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: PensionAnnualAllowancesViewModel = pensionsCYAModel.pensionsAnnualAllowances
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(taxPaidByPensionProvider = Some(amount)))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                //TODO: redirct to the annual allowance pension scheme tax reference page
                Redirect(PensionsSummaryController.show(taxYear))
              }

            } else {
              Future.successful(Redirect(PensionProviderPaidTaxController.show(taxYear)))
            }

          case _ =>
            //TODO: redirect to the annual allowances CYA page when available
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }

      }
    )
  }

}
