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
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.pension.statebenefits.IncomeFromPensionsViewModel
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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionStartDateController @Inject() (actionsProvider: ActionsProvider,
                                                 service: PensionSessionService,
                                                 view: StatePensionStartDateView,
                                                 formsProvider: FormsProvider,
                                                 mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.stateBenefitDateForm

    validateFlow(journey, StatePaymentsStartDatePage, taxYear) {
      val form = journey.statePension
        .flatMap(_.startDate)
        .fold(formProvider) { date =>
          val dateModel =
            DateModel(
              date.getDayOfMonth.toString,
              date.getMonthValue.toString,
              date.getYear.toString
            )
          formProvider.fill(dateModel)
        }
      Future.successful(Ok(view(form, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.incomeFromPensions
    val formProvider = formsProvider.stateBenefitDateForm.bindFromRequest()

    validateFlow(journey, StatePaymentsStartDatePage, taxYear) {
      // TODO: The validations on date forms needs rewriting. We shouldn't be using `.get` here. Will be fixed in SASS-7591.
      formProvider
        .copy(errors = DateForm.verifyDate(formProvider.get, "incomeFromPensions.stateBenefitStartDate"))
        .fold(
          formErrors => Future.successful(BadRequest(view(formErrors, taxYear))),
          answer => {
            val updatedJourney = updateJourney(journey, answer)
            service
              .upsertSession(refreshSessionModel(updatedJourney))
              .onSuccess(determineRedirectFrom(updatedJourney, taxYear))
          }
        )
    }
  }

  private def updateJourney(journey: IncomeFromPensionsViewModel, answer: DateModel): IncomeFromPensionsViewModel = {
    val updatedStatePensionJourney = journey.statePension
      .map { sp =>
        sp.copy(
          startDateQuestion = true.some,
          startDate = answer.toLocalDate.some
        )
      }
    journey.copy(statePension = updatedStatePensionJourney)
  }

  private def determineRedirectFrom(journey: IncomeFromPensionsViewModel, taxYear: Int): Result =
    if (journey.isStatePensionFinished) cyaPageRedirect(taxYear)
    else claimLumpSumRedirect(taxYear)

}
