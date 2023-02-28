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

package controllers.pensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.TaxYearAction.taxYearAction
import controllers.predicates.AuthorisedAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.PensionsSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PensionsSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                           appConfig: AppConfig,
                                           authAction: AuthorisedAction,
                                           pensionSessionService: PensionSessionService,
                                           errorHandler: ErrorHandler,
                                           clock: Clock,
                                           ec: ExecutionContext,
                                           pensionSummaryView: PensionsSummaryView) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (_, prior) =>
      pensionSessionService.getPensionSessionData(taxYear, request.user) flatMap {
        case Right(optPensionsUserData) => optPensionsUserData match {
          case Some(_) =>
            Future.successful(Ok(pensionSummaryView(taxYear, prior)))
          case _ =>
            pensionSessionService.createOrUpdateSessionData(request.user, PensionsCYAModel.emptyModels, taxYear,
                          isPriorSubmission = false)(errorHandler.handleError(INTERNAL_SERVER_ERROR)) {
              Ok(pensionSummaryView(taxYear, prior))
            }
        }
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
