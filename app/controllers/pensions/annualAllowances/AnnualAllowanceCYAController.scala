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

package controllers.pensions.annualAllowances

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateAnnualAllowanceSessionFromPrior
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AnnualAllowanceService
import services.redirects.AnnualAllowancesPages.CYAPage
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.annualAllowances.AnnualAllowancesCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnnualAllowanceCYAController @Inject() (auditProvider: AuditActionsProvider,
                                              view: AnnualAllowancesCYAView,
                                              service: AnnualAllowanceService,
                                              errorHandler: ErrorHandler,
                                              cc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = auditProvider.annualAllowancesViewAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      Future.successful(Ok(view(taxYear, data.pensions.pensionsAnnualAllowances)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.annualAllowancesUpdateAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { sessionData =>
      if (sessionDataDifferentThanPriorData(sessionData.pensions, request.pensions)) {
        service.saveAnswers(request.user, TaxYear(taxYear)).map {
          case Left(_)  => errorHandler.internalServerError()
          case Right(_) => Redirect(PensionsSummaryController.show(taxYear))
        }
      } else {
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  private def sessionDataDifferentThanPriorData(sessionData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean =
    priorData match {
      case None        => true
      case Some(prior) => !sessionData.pensionsAnnualAllowances.equals(generateAnnualAllowanceSessionFromPrior(prior))
    }
}
