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
import models.pension.charges.OverseasRefundPensionScheme.allSchemesFinished
import models.pension.charges.ShortServiceRefundsViewModel
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.ShortServiceRefundsPages.NonUkTaxRefundsAmountPage
import services.redirects.ShortServiceRefundsRedirects.{cyaPageRedirect, refundSchemeRedirect, refundSummaryRedirect}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.validateFlow
import views.html.pensions.shortServiceRefunds.NonUkTaxRefundsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonUkTaxRefundsController @Inject() (actionsProvider: ActionsProvider,
                                           service: PensionSessionService,
                                           view: NonUkTaxRefundsView,
                                           formsProvider: FormsProvider,
                                           mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val answers      = request.sessionData.pensions.shortServiceRefunds
    val formProvider = formsProvider.nonUkTaxRefundsForm(request.user)

    validateFlow(answers, NonUkTaxRefundsAmountPage, taxYear) {
      val form = answers.shortServiceRefundTaxPaid.fold(formProvider)(formProvider.fill(_, answers.shortServiceRefundTaxPaidCharge))

      Future.successful(Ok(view(form, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val journey      = request.sessionData.pensions.shortServiceRefunds
    val formProvider = formsProvider.nonUkTaxRefundsForm(request.user)

    validateFlow(journey, NonUkTaxRefundsAmountPage, taxYear) {
      formProvider
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(view(errors, taxYear))),
          answer => {
            val (bool, maybeAmount) = answer
            val updatedJourney      = journey.copy(shortServiceRefundTaxPaid = bool.some, shortServiceRefundTaxPaidCharge = maybeAmount)
            service
              .upsertSession(updateSessionModel(updatedJourney))
              .onSuccess(determineRedirectFrom(updatedJourney, taxYear))
          }
        )
    }
  }

  private def determineRedirectFrom(journey: ShortServiceRefundsViewModel, taxYear: Int): Result =
    if (journey.isFinished) cyaPageRedirect(taxYear)
    else if (allSchemesFinished(journey.refundPensionScheme)) refundSummaryRedirect(taxYear)
    else refundSchemeRedirect(taxYear, None)

  private def updateSessionModel(updatedJourney: ShortServiceRefundsViewModel)(implicit
      request: UserSessionDataRequest[AnyContent]): PensionsUserData = {
    val updatedSession = request.sessionData.pensions.copy(shortServiceRefunds = updatedJourney)

    request.sessionData.copy(pensions = updatedSession)
  }
}
