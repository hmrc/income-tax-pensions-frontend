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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromPensions.routes.StatePensionLumpSumStartDateController
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.StatePensionPages.TaxOnStatePensionLumpSumPage
import services.redirects.StatePensionRedirects.{cyaPageCall, journeyCheck, statePensionIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.TaxPaidOnStatePensionLumpSumView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TaxPaidOnStatePensionLumpSumController @Inject() (actionsProvider: ActionsProvider,
                                                        pensionSessionService: PensionSessionService,
                                                        view: TaxPaidOnStatePensionLumpSumView,
                                                        errorHandler: ErrorHandler,
                                                        formsProvider: FormsProvider,
                                                        mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock)
    extends FrontendController(mcc)
    with SessionHelper
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit sessionData =>
    val checkRedirect = journeyCheck(TaxOnStatePensionLumpSumPage, _, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val taxPaidQuestion = data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaidQuestion)
      val taxPaid         = data.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid)

      (taxPaidQuestion, taxPaid) match {
        case (Some(yesNo), taxPaid) =>
          Future.successful(Ok(view(formsProvider.taxPaidOnStatePensionLumpSum(sessionData.user).fill((yesNo, taxPaid)), taxYear)))
        case _ =>
          Future.successful(Ok(view(formsProvider.taxPaidOnStatePensionLumpSum(sessionData.user), taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit sessionData =>
    val checkRedirect = journeyCheck(TaxOnStatePensionLumpSumPage, _, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      formsProvider
        .taxPaidOnStatePensionLumpSum(sessionData.user)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          yesNoAmount =>
            (yesNoAmount._1, yesNoAmount._2) match {
              case (true, amount) => updateSessionData(data, yesNo = true, amount, taxYear)
              case (false, _)     => updateSessionData(data, yesNo = false, None, taxYear)
            }
        )
    }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, yesNo: Boolean, taxPaid: Option[BigDecimal], taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {
    val viewModel: IncomeFromPensionsViewModel = pensionUserData.pensions.incomeFromPensions
    val updateStatePensionLumpSum: StateBenefitViewModel = viewModel.statePensionLumpSum match {
      case Some(value) => value.copy(taxPaidQuestion = Some(yesNo), taxPaid = if (yesNo) taxPaid else None)
      case _           => StateBenefitViewModel(taxPaidQuestion = Some(yesNo), taxPaid = if (yesNo) taxPaid else None)
    }

    val updatedCyaModel = pensionUserData.pensions.copy(incomeFromPensions = viewModel.copy(statePensionLumpSum = Some(updateStatePensionLumpSum)))

    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      statePensionIsFinishedCheck(updatedCyaModel.incomeFromPensions, taxYear, StatePensionLumpSumStartDateController.show(taxYear))
    }
  }
}
