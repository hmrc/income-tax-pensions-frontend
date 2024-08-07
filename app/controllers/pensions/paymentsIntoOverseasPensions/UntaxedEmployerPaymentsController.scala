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
import forms.FormsProvider
import models.pension.charges.OverseasPensionScheme
import models.pension.pages.UntaxedEmployerPayments
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.UntaxedEmployerPaymentsService
import services.redirects.PaymentsIntoOverseasPensionsPages.UntaxedEmployerPaymentsPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{indexCheckThenJourneyCheck, schemeIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.paymentsIntoOverseasPensions.UntaxedEmployerPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UntaxedEmployerPaymentsController @Inject() (actionsProvider: ActionsProvider,
                                                   view: UntaxedEmployerPaymentsView,
                                                   service: UntaxedEmployerPaymentsService,
                                                   formsProvider: FormsProvider,
                                                   errorHandler: ErrorHandler,
                                                   cc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async {
    implicit request =>
      val piopSessionData = request.sessionData.pensions.paymentsIntoOverseasPensions

      indexCheckThenJourneyCheck(request.sessionData, pensionSchemeIndex, UntaxedEmployerPaymentsPage, taxYear) { _: OverseasPensionScheme =>
        Future.successful(
          Ok(
            view(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, piopSessionData, formsProvider.untaxedEmployerPayments(request.user.isAgent)))))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] =
    actionsProvider.authoriseWithSession(taxYear).async { implicit request =>
      val piopSessionData = request.sessionData.pensions.paymentsIntoOverseasPensions

      indexCheckThenJourneyCheck(request.sessionData, pensionSchemeIndex, UntaxedEmployerPaymentsPage, taxYear) { _: OverseasPensionScheme =>
        formsProvider
          .untaxedEmployerPayments(request.user.isAgent)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, piopSessionData, formWithErrors)))),
            amount =>
              service.updateUntaxedEmployerPayments(request.sessionData, amount, pensionSchemeIndex).map {
                case Left(_) => errorHandler.internalServerError()
                case Right(_) =>
                  schemeIsFinishedCheck(
                    request.sessionData.pensions.paymentsIntoOverseasPensions.schemes,
                    pensionSchemeIndex.getOrElse(0),
                    taxYear,
                    PensionReliefTypeController.show(taxYear, pensionSchemeIndex)
                  )
              }
          )
      }
    }
}
