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

package controllers.pensions.incomeFromOverseasPensions

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus
import controllers.pensions.routes.{OverseasPensionsSummaryController, PensionsSummaryController}
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateIncomeFromOverseasPensionsCyaFromPrior
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeFromOverseasPensionsService
import services.redirects.IncomeFromOverseasPensionsPages.CYAPage
import services.redirects.IncomeFromOverseasPensionsRedirects.journeyCheck
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeFromOverseasPensionsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                         view: IncomeFromOverseasPensionsCYAView,
                                                         service: IncomeFromOverseasPensionsService,
                                                         errorHandler: ErrorHandler,
                                                         cc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = auditProvider.incomeFromOverseasPensionsViewAuditing(taxYear) async { implicit request =>
    val cya           = Some(request.sessionData)
    val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, cya, PensionOverseasIncomeStatus.show(taxYear))(checkRedirect) { data =>
      Future.successful(Ok(view(taxYear, data.pensions.incomeFromOverseasPensions)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.incomeFromOverseasPensionsUpdateAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), PensionOverseasIncomeStatus.show(taxYear))(checkRedirect) { sessionData =>
      if (shouldSaveAnswers(sessionData.pensions, request.maybePrior)) {
        service.saveAnswers(request.user, TaxYear(taxYear)).map {
          case Left(_)  => errorHandler.internalServerError()
          case Right(_) => Redirect(OverseasPensionsSummaryController.show(taxYear))
        }
      } else Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def shouldSaveAnswers(sessionData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean =
    priorData.fold(ifEmpty = true) { prior =>
      !sessionData.incomeFromOverseasPensions.equals(generateIncomeFromOverseasPensionsCyaFromPrior(prior))
    }
}
