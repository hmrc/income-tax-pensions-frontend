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

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus
import controllers.pensions.routes.{OverseasPensionsSummaryController, PensionsSummaryController}
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.IncomeFromOverseasPensionsPages.CYAPage
import services.redirects.IncomeFromOverseasPensionsRedirects.journeyCheck
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{PensionIncomeService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeFromOverseasPensionsCYAController @Inject()(auditProvider: AuditActionsProvider,
                                                        view: IncomeFromOverseasPensionsCYAView,
                                                        pensionIncomeService: PensionIncomeService,
                                                        errorHandler: ErrorHandler)
                                                       (implicit val mcc: MessagesControllerComponents,
                                                        appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = auditProvider.incomeFromOverseasPensionsViewAuditing(taxYear) async {
    implicit request =>
    val cya = Some(request.pensionsUserData)
      val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, cya, PensionOverseasIncomeStatus.show(taxYear))(checkRedirect) {
        data => Future.successful(Ok(view(taxYear, data.pensions.incomeFromOverseasPensions)))
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = auditProvider.incomeFromOverseasPensionsUpdateAuditing(taxYear) async { implicit request =>
    //TODO: missing the comparison of session with Prior data
    pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(request.user, taxYear).map {
      case Left(_) =>
        errorHandler.internalServerError()
      case Right(_) => Redirect(OverseasPensionsSummaryController.show(taxYear))
    }
  }
}
