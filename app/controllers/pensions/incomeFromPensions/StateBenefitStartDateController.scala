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
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.ActionsProvider
import filters.InputFilters
import forms.DateForm.DateModel
import forms.{DateForm, FormsProvider}
import models.mongo.PensionsUserData
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.StateBenefitsStartDateView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StateBenefitStartDateController @Inject()(actionsProvider: ActionsProvider,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler,
                                                view: StateBenefitsStartDateView,
                                                formProvider: FormsProvider)
                                               (implicit val mcc: MessagesControllerComponents,
                                                appConfig: AppConfig,
                                                clock: Clock,
                                                ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {
    implicit userSessionDataRequest =>
      userSessionDataRequest.pensionsUserData.pensions.incomeFromPensions.statePension.fold {
        Redirect(PensionsSummaryController.show(taxYear))
      } { sP =>
        sP.startDate.fold {
          Ok(view(formProvider.stateBenefitDateForm, taxYear))
        } { startDate =>
          val filledForm: Form[DateModel] = formProvider.stateBenefitDateForm.fill(DateModel(
            startDate.getDayOfMonth.toString, startDate.getMonthValue.toString, startDate.getYear.toString)
          )
          Ok(view(filledForm, taxYear))
        }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      formProvider.stateBenefitDateForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        newStartDate =>
          userSessionDataRequest.pensionsUserData.pensions.incomeFromPensions.statePension.fold {
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          } { sP =>
            val updatedModel: PensionsUserData =
              userSessionDataRequest.pensionsUserData.copy(pensions =
                userSessionDataRequest.pensionsUserData.pensions.copy(incomeFromPensions =
                  userSessionDataRequest.pensionsUserData.pensions.incomeFromPensions.copy(statePension =
                    Some(sP.copy(startDateQuestion = Some(true), startDate = Some(newStartDate.toLocalDate)))
                  )))
            pensionSessionService.createOrUpdateSessionData(updatedModel).map {
              case Right(_) => Ok(view(formProvider.stateBenefitDateForm, taxYear))
              case _ => errorHandler.internalServerError()
            }
          }
      )
  }

}
