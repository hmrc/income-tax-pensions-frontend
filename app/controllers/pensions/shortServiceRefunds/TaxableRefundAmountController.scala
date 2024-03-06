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

package controllers.pensions.shortServiceRefunds

import cats.implicits.catsSyntaxOptionId
import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.charges.ShortServiceRefundsViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ShortServiceRefundsService
import services.redirects.ShortServiceRefundsRedirects.{cyaPageRedirect, nonUkTaxRefundsRedirect}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import views.html.pensions.shortServiceRefunds.TaxableRefundAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxableRefundAmountController @Inject() (actionsProvider: ActionsProvider,
                                               service: ShortServiceRefundsService,
                                               view: TaxableRefundAmountView,
                                               formsProvider: FormsProvider,
                                               mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit request =>
    val answers      = request.pensionsUserData.pensions.shortServiceRefunds
    val formProvider = formsProvider.shortServiceTaxableRefundForm(request.user)
    val form         = answers.shortServiceRefund.fold(ifEmpty = formProvider)(formProvider.fill(_, answers.shortServiceRefundCharge))

    Ok(view(form, taxYear))
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val journey      = request.pensionsUserData.pensions.shortServiceRefunds
    val formProvider = formsProvider.shortServiceTaxableRefundForm(request.user)

    formProvider
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(view(errors, taxYear))),
        answer =>
          service
            .upsertSession(updateSessionModel(answer._1, answer._2))
            .onSuccess(redirectTo(journey, answer._1, taxYear))
      )
  }

  private def updateSessionModel(bool: Boolean, maybeAmount: Option[BigDecimal])(implicit
      request: UserSessionDataRequest[AnyContent]): PensionsUserData = {
    val journey = request.pensionsUserData.pensions.shortServiceRefunds
    val updatedJourney =
      if (bool) journey.copy(shortServiceRefund = true.some, shortServiceRefundCharge = maybeAmount)
      else ShortServiceRefundsViewModel(shortServiceRefund = false.some)

    val updatedSession = request.pensionsUserData.pensions.copy(shortServiceRefunds = updatedJourney)

    request.pensionsUserData.copy(pensions = updatedSession)
  }

  private def redirectTo(journey: ShortServiceRefundsViewModel, bool: Boolean, taxYear: Int): Result =
    if (journey.isFinished || !bool) cyaPageRedirect(taxYear)
    else nonUkTaxRefundsRedirect(taxYear)

}
