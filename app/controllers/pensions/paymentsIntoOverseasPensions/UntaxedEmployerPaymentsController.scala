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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoOverseasPensions.routes.PensionReliefTypeController
import controllers.predicates.actions.ActionsProvider
import controllers.validatedSchemes
import forms.FormsProvider
import models.pension.charges.Relief
import models.pension.pages.UntaxedEmployerPayments
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PaymentsIntoOverseasPensionsService
import services.redirects.PaymentsIntoOverseasPensionsPages.UntaxedEmployerPaymentsPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, redirectForSchemeLoop}
import services.redirects.SimpleRedirectService.isFinishedCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.paymentsIntoOverseasPensions.UntaxedEmployerPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UntaxedEmployerPaymentsController @Inject()(actionsProvider: ActionsProvider,
                                                  pageView: UntaxedEmployerPaymentsView,
                                                  paymentsIntoOverseasService: PaymentsIntoOverseasPensionsService,
                                                  formsProvider: FormsProvider,
                                                  errorHandler: ErrorHandler)
                                                 (implicit val cc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val piopSessionData = sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions

    indexCheckThenJourneyCheck(sessionUserData.pensionsUserData, pensionSchemeIndex, UntaxedEmployerPaymentsPage, taxYear) { _: Relief =>
      Future.successful(
        Ok(pageView(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, piopSessionData,
          formsProvider.untaxedEmployerPayments(sessionUserData.user.isAgent)))))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit sessionUserData =>
      val piopSessionData = sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions
      val redirectLocation = PensionReliefTypeController.show(taxYear, pensionSchemeIndex)

      indexCheckThenJourneyCheck(sessionUserData.pensionsUserData, pensionSchemeIndex, UntaxedEmployerPaymentsPage, taxYear) { _: Relief =>
        formsProvider.untaxedEmployerPayments(sessionUserData.user.isAgent).bindFromRequest().fold(
          formWithErrors => {
            Future.successful(
              BadRequest(pageView(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, piopSessionData, formWithErrors))))
          },
          amount => {
            paymentsIntoOverseasService.updateUntaxedEmployerPayments(sessionUserData.pensionsUserData, amount, pensionSchemeIndex).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(_) =>
                isFinishedCheck(sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions, taxYear, redirectLocation, cyaPageCall)
            }
          }
        )
      }
    }
  }
}
