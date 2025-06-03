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
import forms.standard.StandardErrorKeys.{EarliestDate, PresentDate}
import models.mongo.PensionsUserData
import models.pension.statebenefits.IncomeFromPensionsViewModel
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.StatePensionPages.StatePensionLumpSumStartDatePage
import services.redirects.StatePensionRedirects.cyaPageRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.incomeFromPensions.StatePensionValidator.validateFlow
import views.html.pensions.incomeFromPensions.StatePensionLumpSumStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionLumpSumStartDateController @Inject() (actionsProvider: ActionsProvider,
                                                        service: PensionSessionService,
                                                        view: StatePensionLumpSumStartDateView,
                                                        formProvider: LocalDateFormProvider,
                                                        mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  private val form: Form[LocalDate] =
    formProvider(
      "statePensionLumpSumStartDate",
      altErrorPrefix = "pensions.statePensionLumpSumStartDate",
      earliestDateAndError = Some((EarliestDate, "pensions.statePensionLumpSumStartDate.error.localDate.tooLongAgo")),
      latestDateAndError = Some((PresentDate, "pensions.statePensionLumpSumStartDate.error.localDate.dateInFuture"))
    )

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journeyData = request.sessionData.pensions.incomeFromPensions
    validateFlow(journeyData, StatePensionLumpSumStartDatePage, taxYear) {
      val filledForm = journeyData.statePensionLumpSum.flatMap(_.startDate).fold(form)(form.fill)
      Future.successful(Ok(view(filledForm, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journeyData = request.sessionData.pensions.incomeFromPensions

    validateFlow(journeyData, StatePensionLumpSumStartDatePage, taxYear) {
      form
        .bindFromRequest()
        .fold(
          formErrors => Future.successful(BadRequest(view(formErrors, taxYear))),
          answer =>
            service
              .upsertSession(updateSessionModel(journeyData, answer))
              .onSuccess(cyaPageRedirect(taxYear))
        )
    }

  }
  private def updateSessionModel(journey: IncomeFromPensionsViewModel, answer: LocalDate)(implicit
      request: UserSessionDataRequest[AnyContent]): PensionsUserData = {
    val updatedLumpSumJourney = journey.statePensionLumpSum.map { lumpSum =>
      lumpSum.copy(
        startDateQuestion = true.some,
        startDate = answer.some
      )
    }
    val updatedPensionsIncome = journey.copy(statePensionLumpSum = updatedLumpSumJourney)

    refreshSessionModel(updatedPensionsIncome)
  }

}
