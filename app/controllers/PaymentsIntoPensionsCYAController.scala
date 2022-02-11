/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import javax.inject.Inject
import models.IncomeTaxUserData
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper

import scala.concurrent.{ExecutionContext, Future}

class PaymentsIntoPensionsCYAController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  inYearAction: InYearAction,
                                                  appConfig: AppConfig,
                                                  pensionSessionService: PensionSessionService,
                                                  ec: ExecutionContext,
                                                  errorHandler: ErrorHandler
                                                 ) extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getAndHandle(taxYear) { (cya, prior) =>
      (cya.map(_.pensions), prior) match {
        case (Some(cyaData), Some(priorData)) =>
          if(cyaData.paymentsIntoPension.isFinished) {
            Future.successful(Ok(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))) //TODO - direct to CYA view
          } else {
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        case (None, Some(priorData)) => Future.successful(Ok(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))) //TODO - direct to CYA view
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

}
