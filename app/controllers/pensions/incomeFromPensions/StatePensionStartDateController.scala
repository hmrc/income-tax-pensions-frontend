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
import forms.standard.LocalDateFormProvider
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.StatePensionPages.StatePaymentsStartDatePage
import services.redirects.StatePensionRedirects._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.incomeFromPensions.StatePensionValidator.validateFlow
import views.html.pensions.incomeFromPensions.StatePensionStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionStartDateController @Inject() (actionsProvider: ActionsProvider,
                                                 service: PensionSessionService,
                                                 view: StatePensionStartDateView,
                                                 formProvider: LocalDateFormProvider,
                                                 mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  private val form: Form[LocalDate] = formProvider("stateBenefitStartDate")

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journeyData = request.sessionData.pensions.incomeFromPensions
    validateFlow(journeyData, StatePaymentsStartDatePage, taxYear) {
      val filledForm: Form[LocalDate] = journeyData.statePension.flatMap(_.startDate).fold(form)(form.fill)
      Future.successful(Ok(view(filledForm, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journeyData = request.sessionData.pensions.incomeFromPensions

    validateFlow(journeyData, StatePaymentsStartDatePage, taxYear) {
      form
        .bindFromRequest()
        .fold(
          formErrors => Future.successful(BadRequest(view(formErrors, taxYear))),
          answer => {
            val updatedJourneyData = updateJourney(journeyData, answer)
            service
              .upsertSession(refreshSessionModel(updatedJourneyData))
              .onSuccess(determineRedirectFrom(updatedJourneyData, taxYear))
          }
        )
    }
  }

  private def updateJourney(journey: IncomeFromPensionsViewModel, answer: LocalDate): IncomeFromPensionsViewModel = {
    val updatedStatePensionJourney = journey.statePension
      .map { sp =>
        sp.copy(
          startDateQuestion = true.some,
          startDate = answer.some
        )
      }
    journey.copy(statePension = updatedStatePensionJourney)
  }

  private def determineRedirectFrom(journey: IncomeFromPensionsViewModel, taxYear: Int): Result =
    if (areStatePensionClaimsComplete(journey)) cyaPageRedirect(taxYear)
    else claimLumpSumRedirect(taxYear)

}
