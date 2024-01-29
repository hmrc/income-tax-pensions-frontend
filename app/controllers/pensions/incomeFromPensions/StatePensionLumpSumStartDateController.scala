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
import controllers.pensions.incomeFromPensions.routes.StatePensionAddToCalculationController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.ActionsProvider
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.pension.statebenefits.StateBenefitViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.StatePensionPages.WhenDidYouGetYourStatePensionLumpSumPage
import services.redirects.StatePensionRedirects.{cyaPageCall, journeyCheck, statePensionIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.StatePensionLumpSumStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionLumpSumStartDateController @Inject() (
    actionsProvider: ActionsProvider,
    pensionSessionService: PensionSessionService,
    view: StatePensionLumpSumStartDateView,
    formsProvider: FormsProvider,
    errorHandler: ErrorHandler)(implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionData =>
    val checkRedirect = journeyCheck(WhenDidYouGetYourStatePensionLumpSumPage, _, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      Future.successful(data.pensions.incomeFromPensions.statePensionLumpSum.fold {
        Redirect(PensionsSummaryController.show(taxYear))
      } { sP: StateBenefitViewModel =>
        sP.startDate.fold {
          Ok(view(formsProvider.statePensionLumpSumStartDateForm, taxYear))
        } { startDate: LocalDate =>
          val filledForm: Form[DateModel] = formsProvider.statePensionLumpSumStartDateForm
            .fill(
              DateModel(
                startDate.getDayOfMonth.toString,
                startDate.getMonthValue.toString,
                startDate.getYear.toString
              ))
          Ok(view(filledForm, taxYear))
        }
      })
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionData =>
    val checkRedirect = journeyCheck(WhenDidYouGetYourStatePensionLumpSumPage, _, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val verifiedForm = formsProvider.statePensionLumpSumStartDateForm.bindFromRequest()
      verifiedForm
        .copy(errors = DateForm.verifyDate(verifiedForm.get, "pensions.statePensionLumpSumStartDate"))
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          formWithStartDate =>
            data.pensions.incomeFromPensions.statePensionLumpSum.fold {
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
            } { sP =>
              val updatedModel =
                data.copy(pensions = data.pensions.copy(incomeFromPensions = data.pensions.incomeFromPensions.copy(statePensionLumpSum =
                  Some(sP.copy(startDateQuestion = Some(true), startDate = Some(formWithStartDate.toLocalDate))))))
              pensionSessionService.createOrUpdateSessionData(updatedModel).map {
                case Right(_) =>
                  statePensionIsFinishedCheck(updatedModel.pensions.incomeFromPensions, taxYear, StatePensionAddToCalculationController.show(taxYear))
                case _ => errorHandler.internalServerError()
              }
            }
        )
    }
  }
}
