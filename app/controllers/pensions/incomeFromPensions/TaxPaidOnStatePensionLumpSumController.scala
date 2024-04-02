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

import cats.implicits.{catsSyntaxOptionId, none}
import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.StatePensionPages.TaxOnStatePensionLumpSumPage
import services.redirects.StatePensionRedirects._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.incomeFromPensions.StatePensionValidator.validateFlow
import views.html.pensions.incomeFromPensions.TaxPaidOnStatePensionLumpSumView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxPaidOnStatePensionLumpSumController @Inject() (actionsProvider: ActionsProvider,
                                                        service: PensionSessionService,
                                                        view: TaxPaidOnStatePensionLumpSumView,
                                                        formsProvider: FormsProvider,
                                                        mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with SessionHelper
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.taxPaidOnStatePensionLumpSum(request.user)

    validateFlow(journey, TaxOnStatePensionLumpSumPage, taxYear) {
      val form = journey.statePensionLumpSum
        .flatMap(_.taxPaidQuestion)
        .fold(formProvider)(formProvider.fill(_, journey.statePensionLumpSum.flatMap(_.taxPaid)))

      Future.successful(Ok(view(form, taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.taxPaidOnStatePensionLumpSum(request.user)

    formProvider
      .bindFromRequest()
      .fold(
        formErrors => Future.successful(BadRequest(view(formErrors, taxYear))),
        answer => {
          val (bool, maybeAmount) = answer
          val updatedJourney      = updateJourneyModel(journey, bool, maybeAmount)
          service
            .upsertSession(refreshSessionModel(updatedJourney))
            .onSuccess(determineRedirectFrom(updatedJourney, taxYear))
        }
      )

  }

  private def updateJourneyModel(journey: IncomeFromPensionsViewModel,
                                 bool: Boolean,
                                 maybeAmount: Option[BigDecimal]): IncomeFromPensionsViewModel = {
    val updatedLumpSum = journey.statePensionLumpSum
      .map { lumpSumAnswers =>
        if (bool) lumpSumAnswers.copy(taxPaidQuestion = true.some, taxPaid = maybeAmount)
        else lumpSumAnswers.copy(taxPaidQuestion = false.some, taxPaid = none[BigDecimal])
      }

    journey.copy(statePensionLumpSum = updatedLumpSum)
  }

  private def determineRedirectFrom(journey: IncomeFromPensionsViewModel, taxYear: Int): Result =
    if (areStatePensionClaimsComplete(journey)) cyaPageRedirect(taxYear)
    else lumpSumStartDateRedirect(taxYear)

}
