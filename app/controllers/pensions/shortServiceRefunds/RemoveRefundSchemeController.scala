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
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.ActionsProvider
import models.mongo.PensionsUserData
import models.pension.charges.OverseasRefundPensionScheme
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.ShortServiceRefundsPages.RemoveRefundSchemePage
import services.redirects.ShortServiceRefundsRedirects.refundSummaryRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.{validateFlow, validateIndex}
import views.html.pensions.shortServiceRefunds.RemoveRefundSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveRefundSchemeController @Inject() (actionsProvider: ActionsProvider,
                                              service: PensionSessionService,
                                              view: RemoveRefundSchemeView,
                                              errorHandler: ErrorHandler,
                                              mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateIndex[RemoveRefundSchemePage](index, answers, taxYear) { validIndex =>
      validateFlow(answers, RemoveRefundSchemePage(), taxYear, validIndex.some) {
        val result = answers
          .refundPensionScheme(validIndex)
          .name
          .map(schemeName => Ok(view(taxYear, schemeName, validIndex.some)))
          .getOrElse(errorHandler.internalServerError())

        Future.successful(result)
      }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateIndex[RemoveRefundSchemePage](index, answers, taxYear) { validIndex =>
      validateFlow(answers, RemoveRefundSchemePage(), taxYear, validIndex.some) {
        val updatedSchemes = answers.refundPensionScheme.patch(validIndex, Nil, 1)
        val updatedModel   = updateSessionModel(request.sessionData, updatedSchemes)

        service
          .upsertSession(updatedModel)
          .onSuccess(refundSummaryRedirect(taxYear))
      }
    }
  }

  private def updateSessionModel(session: PensionsUserData, updatedSchemes: Seq[OverseasRefundPensionScheme]): PensionsUserData = {
    val updatedJourney  = session.pensions.shortServiceRefunds.copy(refundPensionScheme = updatedSchemes)
    val updatedPensions = session.pensions.copy(shortServiceRefunds = updatedJourney)
    session.copy(pensions = updatedPensions)
  }

}
