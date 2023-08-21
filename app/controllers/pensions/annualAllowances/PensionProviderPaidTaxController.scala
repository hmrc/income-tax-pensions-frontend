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

package controllers.pensions.annualAllowances

import config.{AppConfig, ErrorHandler}
import controllers.pensions.annualAllowances.routes.PensionSchemeTaxReferenceController
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider.pensionProviderPaidTaxForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.AnnualAllowancesPages.PensionProviderPaidTaxPage
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.annualAllowances.PensionProviderPaidTaxView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionProviderPaidTaxController @Inject()(actionsProvider: ActionsProvider,
                                                 pensionSessionService: PensionSessionService,
                                                 view: PensionProviderPaidTaxView,
                                                 errorHandler: ErrorHandler)
                                                (implicit val mcc: MessagesControllerComponents,
                                                 appConfig: AppConfig,
                                                 clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataForInYear(taxYear) async {
    implicit sessionData =>
      val checkRedirect = journeyCheck(PensionProviderPaidTaxPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>
          val providePaidAnnualAllowanceQuestion = data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion
          val taxPaid = data.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider

          (providePaidAnnualAllowanceQuestion, taxPaid) match {
            case (Some(bool), amount) =>
              Future.successful(Ok(view(pensionProviderPaidTaxForm(sessionData.user.isAgent).fill((bool, amount)), taxYear)))
            case _ =>
              Future.successful(Ok(view(pensionProviderPaidTaxForm(sessionData.user.isAgent), taxYear)))
          }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataForInYear(taxYear) async {
    implicit sessionData =>
      val checkRedirect = journeyCheck(PensionProviderPaidTaxPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>
          pensionProviderPaidTaxForm(sessionData.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
            yesNoAmount => {
              (yesNoAmount._1, yesNoAmount._2) match {
                case (true, amount) => updateSessionData(data, yesNo = true, amount, taxYear)
                case (false, _) => updateSessionData(data, yesNo = false, None, taxYear)
              }
            }
          )
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {

    val viewModel: PensionAnnualAllowancesViewModel = pensionUserData.pensions.pensionsAnnualAllowances
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(pensionsAnnualAllowances = {
      if (yesNo)
        viewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(true), taxPaidByPensionProvider = amount)
      else
        viewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false), taxPaidByPensionProvider = None, pensionSchemeTaxReferences = None)
    })
    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear,
      pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      isFinishedCheck(
        updatedCyaModel.pensionsAnnualAllowances, taxYear,
        PensionSchemeTaxReferenceController.show(taxYear, None),
        cyaPageCall)
    }
  }

}
