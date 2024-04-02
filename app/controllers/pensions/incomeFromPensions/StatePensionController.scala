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

import cats.implicits.catsSyntaxOptionId
import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.StatePensionRedirects.{claimLumpSumRedirect, cyaPageRedirect, statePensionStartDateRedirect}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.StatePensionView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionController @Inject() (actionsProvider: ActionsProvider,
                                        service: PensionSessionService,
                                        view: StatePensionView,
                                        formsProvider: FormsProvider,
                                        mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.statePensionForm(request.user)
    val form = journey.statePension
      .flatMap(_.amountPaidQuestion)
      .fold(ifEmpty = formProvider)(formProvider.fill(_, journey.statePension.flatMap(_.amount)))

    Ok(view(form, taxYear))
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.statePensionForm(request.user)

    formProvider
      .bindFromRequest()
      .fold(
        formErrors => Future.successful(BadRequest(view(formErrors, taxYear))),
        answer => {
          val (bool, maybeAmount) = answer
          val updatedJourney      = updateSessionModel(journey, bool, maybeAmount)
          service
            .upsertSession(refreshSessionModel(updatedJourney))
            .onSuccess(determineRedirectFrom(updatedJourney, bool, taxYear))
        }
      )
  }

  private def updateSessionModel(journey: IncomeFromPensionsViewModel,
                                 bool: Boolean,
                                 maybeAmount: Option[BigDecimal]): IncomeFromPensionsViewModel = {
    def runUpdateFromBase(base: StateBenefitViewModel): StateBenefitViewModel =
      if (bool) base.copy(amountPaidQuestion = true.some, amount = maybeAmount)
      else base.void.copy(amountPaidQuestion = false.some)

    val updatedStatePension = journey.statePension
      .fold(ifEmpty = runUpdateFromBase(StateBenefitViewModel.empty))(runUpdateFromBase)

    journey.copy(statePension = updatedStatePension.some)
  }

  private def determineRedirectFrom(journey: IncomeFromPensionsViewModel, bool: Boolean, taxYear: Int): Result =
    if (areStatePensionClaimsComplete(journey)) cyaPageRedirect(taxYear)
    else if (bool) statePensionStartDateRedirect(taxYear)
    else claimLumpSumRedirect(taxYear)

}
