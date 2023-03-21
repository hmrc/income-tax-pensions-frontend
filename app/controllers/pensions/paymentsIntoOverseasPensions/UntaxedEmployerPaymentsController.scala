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
import controllers.predicates.{ActionsProvider, AuthorisedAction}
import controllers.validateScheme
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.pages.UntaxedEmployerPayments
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{PaymentsIntoOverseasPensionsService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.UntaxedEmployerPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UntaxedEmployerPaymentsController @Inject()(authAction: AuthorisedAction,
                                                  actionsProvider: ActionsProvider,
                                                  pageView: UntaxedEmployerPaymentsView,
                                                  paymentsIntoOverseasService: PaymentsIntoOverseasPensionsService,
                                                  pensionSessionService: PensionSessionService,
                                                  formsProvider: FormsProvider,
                                                  errorHandler: ErrorHandler)(implicit val cc: MessagesControllerComponents,
                                                                              appConfig: AppConfig,
                                                                              clock: Clock,
                                                                              ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {


  val outOfBoundsRedirect: Int => Result = (taxYear: Int) => Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit sessionUserData =>

    validateScheme(pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.map(_.customerReferenceNumberQuestion)) match {
      case Left(_) =>
        //        outOfBoundsRedirect(taxYear)
        Ok(pageView(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions, formsProvider.untaxedEmployerPayments(sessionUserData.user.isAgent))))
      case Right(_) => Ok(
        pageView(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions, formsProvider.untaxedEmployerPayments(sessionUserData.user.isAgent))))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit sessionUserData =>

      validateScheme(pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.map(_.customerReferenceNumberQuestion)) match {
        case Left(_) => Future.successful(outOfBoundsRedirect(taxYear))
        case Right(_) => formsProvider.untaxedEmployerPayments(sessionUserData.user.isAgent).bindFromRequest().fold(
          formWithErrors => {
            Future.successful(
              BadRequest(pageView(UntaxedEmployerPayments(taxYear, pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions, formWithErrors))))
          }, amount => {
            paymentsIntoOverseasService.updateUntaxedEmployerPayments(sessionUserData.pensionsUserData, amount, pensionSchemeIndex).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(userData) =>
                Redirect(getRedirectCall(
                  taxYear,
                  Some(pensionSchemeIndex.getOrElse(userData.pensions.paymentsIntoOverseasPensions.reliefs.size - 1))))
            }
          }
        )
      }
    }
  }


  private def getRedirectCall(taxYear: Int,
                              pensionSchemeIndex: Option[Int]): Call = {
    controllers.pensions.paymentsIntoOverseasPensions.routes.UntaxedEmployerPaymentsController.show(taxYear, pensionSchemeIndex)
  }
}
