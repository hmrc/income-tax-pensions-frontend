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
import controllers.pensions.annualAllowances.routes._
import controllers.predicates.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowances.AboveReducedAnnualAllowanceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AboveReducedAnnualAllowanceController @Inject()(actionsProvider: ActionsProvider,
                                                      pensionSessionService: PensionSessionService,
                                                      view: AboveReducedAnnualAllowanceView,
                                                      errorHandler: ErrorHandler,
                                                      formsProvider: FormsProvider
                                                     )(implicit val cc: MessagesControllerComponents,
                                                       appConfig: AppConfig,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>

      val reducedAnnualAllowanceQuestion = sessionData.pensionsUserData.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion
      val aboveAnnualAllowanceQuestion = sessionData.pensionsUserData.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion
      val amount = sessionData.pensionsUserData.pensions.pensionsAnnualAllowances.aboveAnnualAllowance

      reducedAnnualAllowanceQuestion match {
        case Some(_) =>

          (aboveAnnualAllowanceQuestion, amount) match {
            case (Some(yesNo), amount) => Future.successful(Ok(view(
              formsProvider.aboveAnnualAllowanceForm(sessionData.user).fill((yesNo, amount): (Boolean, Option[BigDecimal])), taxYear)))
            case _ =>
              Future.successful(Ok(view(formsProvider.aboveAnnualAllowanceForm(sessionData.user), taxYear)))
          }

        case None => Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
      }
  }


  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>

      sessionData.pensionsUserData.pensions.pensionsAnnualAllowances.reducedAnnualAllowanceQuestion match {
        case Some(_) =>

          formsProvider.aboveAnnualAllowanceForm(sessionData.user).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
            yesNoAmount => {
              (yesNoAmount._1, yesNoAmount._2) match {
                case (true, amount) => updateSessionData(sessionData.pensionsUserData, yesNo = true, amount, taxYear)
                case (false, _) => updateSessionData(sessionData.pensionsUserData, yesNo = false, None, taxYear)
              }
            }
          )

        case None => Future.successful(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
      }
  }


  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {

    val viewModel: PensionAnnualAllowancesViewModel = pensionUserData.pensions.pensionsAnnualAllowances
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(pensionsAnnualAllowances = {
      if (yesNo) viewModel.copy(aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = amount)
      else viewModel.copy(
        aboveAnnualAllowanceQuestion = Some(false), aboveAnnualAllowance = None,
        pensionProvidePaidAnnualAllowanceQuestion = None, taxPaidByPensionProvider = None, pensionSchemeTaxReferences = None)
    })

    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission
    )(errorHandler.internalServerError()) {
      Redirect(
        if (yesNo) PensionProviderPaidTaxController.show(taxYear)
        else AnnualAllowanceCYAController.show(taxYear)
      )
    }
  }
}
