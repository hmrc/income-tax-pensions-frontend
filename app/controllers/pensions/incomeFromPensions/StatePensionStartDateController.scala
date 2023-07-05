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
import controllers.pensions.incomeFromPensions.routes.StatePensionLumpSumController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.ActionsProvider
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.mongo.PensionsUserData
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.IncomeFromPensionsPages.WhenDidYouStartGettingStatePaymentsPage
import services.redirects.IncomeFromPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.StatePensionStartDateView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatePensionStartDateController @Inject()(actionsProvider: ActionsProvider,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler,
                                                view: StatePensionStartDateView,
                                                formProvider: FormsProvider)
                                               (implicit val mcc: MessagesControllerComponents,
                                                appConfig: AppConfig,
                                                ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest: UserSessionDataRequest[AnyContent] => {
      val checkRedirect = journeyCheck(WhenDidYouStartGettingStatePaymentsPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(userSessionDataRequest.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data: PensionsUserData =>
          Future.successful(showStartDate(taxYear, data))
      }
    }
  }

  private def showStartDate(taxYear: Int, data: PensionsUserData)(implicit userSessionDataRequest: UserSessionDataRequest[AnyContent]): Result = {
    data.pensions.incomeFromPensions.statePension.fold {
      Redirect(PensionsSummaryController.show(taxYear))
    } { sP =>
      sP.startDate.fold {
        Ok(view(formProvider.stateBenefitDateForm, taxYear))
      } { startDate => {
        val filledForm: Form[DateModel] = formProvider.stateBenefitDateForm.fill(DateModel(
          startDate.getDayOfMonth.toString, startDate.getMonthValue.toString, startDate.getYear.toString)
        )
        Ok(view(filledForm, taxYear))
      }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      val checkRedirect = journeyCheck(WhenDidYouStartGettingStatePaymentsPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(userSessionDataRequest.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val verifiedForm = formProvider.stateBenefitDateForm.bindFromRequest()
      verifiedForm.copy(errors = DateForm.verifyDate(verifiedForm.get, "incomeFromPensions.stateBenefitStartDate")).fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        newStartDate =>
          data.pensions.incomeFromPensions.statePension.fold {
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          } { sP =>
            val updatedModel: PensionsUserData =
              data.copy(pensions =
                data.pensions.copy(incomeFromPensions =
                  data.pensions.incomeFromPensions.copy(statePension =
                    Some(sP.copy(startDateQuestion = Some(true), startDate = Some(newStartDate.toLocalDate)))
                  )))
            pensionSessionService.createOrUpdateSessionData(updatedModel).map {
              case Right(_) => Redirect(StatePensionLumpSumController.show(taxYear))
              case _ => errorHandler.internalServerError()
            }
          }
      )
    }
  }

}
