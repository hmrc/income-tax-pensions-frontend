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

package controllers.pensions.lifetimeAllowances

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionChargesService
import services.redirects.LifetimeAllowancesPages.CYAPage
import services.redirects.LifetimeAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.lifetimeAllowances.LifetimeAllowanceCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LifetimeAllowanceCYAController @Inject()(auditProvider: AuditActionsProvider,
                                               view: LifetimeAllowanceCYAView,
                                               pensionChargesService: PensionChargesService,
                                               errorHandler: ErrorHandler)
                                              (implicit val mcc: MessagesControllerComponents,
                                               appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = auditProvider.lifetimeAllowancesViewAuditing(taxYear) async {
    implicit request =>
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data => Future.successful(Ok(view(taxYear, data.pensions.pensionLifetimeAllowances)))
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.lifetimeAllowancesUpdateAuditing(taxYear) async {
    implicit request =>
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        sessionData =>
          if (sessionDataDifferentThanPriorData(sessionData.pensions, request.pensions)) {
            pensionChargesService.saveLifetimeAllowancesViewModel(request.user, taxYear).map {
              case Right(_) =>
                //TODO: investigate the use of the previously used pensionSessionService.clear
                Redirect(PensionsSummaryController.show(taxYear))
              case Left(_) =>
                logger.info("[submit] Failed to create or update session")
                errorHandler.handleError(BAD_REQUEST)
            }
          } else {
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
      }
  }

  private def sessionDataDifferentThanPriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
