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
import forms.{No, NoButHasAgreedToPay, PensionProviderPaidTaxAnswers, PensionProviderPaidTaxQuestionForm, Yes}
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowance.PensionProviderPaidTaxView
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowance.routes._
import controllers.predicates.TaxYearAction.taxYearAction

import javax.inject.Inject
import scala.concurrent.Future

class PensionProviderPaidTaxController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 pensionProviderPaidTaxView: PensionProviderPaidTaxView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport {

  def providerPaidTaxForm(implicit user: User): Form[PensionProviderPaidTaxAnswers] = PensionProviderPaidTaxQuestionForm.providerPaidTaxForm(
    missingInputError = s"pensions.pensionProviderPaidTax.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion match {
          case Some(question) =>
            val prefillValue = question match {
              case "Yes" => Yes
              case "No" => No
              case "NoButHasAgreedToPay" => NoButHasAgreedToPay
            }
            Future.successful(Ok(pensionProviderPaidTaxView(providerPaidTaxForm(request.user).fill(prefillValue), taxYear)))
          case None =>
            Future.successful(Ok(pensionProviderPaidTaxView(providerPaidTaxForm(request.user), taxYear)))
        }
      case _ =>
        //TODO" navigate to CYA controller
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    providerPaidTaxForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionProviderPaidTaxView(formWithErrors, taxYear))),
      valueFieldName => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel = pensionsCYAModel.pensionsAnnualAllowances

            val updatedCyaModel = pensionsCYAModel.copy(pensionsAnnualAllowances = viewModel.copy(
              pensionProvidePaidAnnualAllowanceQuestion = Some(valueFieldName.toString),
              taxPaidByPensionProvider = if (valueFieldName.toString == No.toString) None else viewModel.taxPaidByPensionProvider
            ))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              valueFieldName match {
                case No =>
                  //TODO redirect to correct page
                  Redirect(PensionsSummaryController.show(taxYear))
                // yes and no, agreed to pay (soft yes)
                case _ => Redirect(TaxPaidByPensionProviderAmountController.show(taxYear))
              }
            }
        }
      }
    )
  }
}
