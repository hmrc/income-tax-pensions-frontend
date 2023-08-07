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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromPensions.routes.{IncomeFromPensionsSummaryController, UkPensionSchemePaymentsController}
import controllers.predicates.auditActions.AuditActionsProvider
import forms.FormUtils
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.redirects.IncomeFromOtherUkPensionsPages.CheckUkPensionIncomeCYAPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentPensionService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromPensions.UkPensionIncomeCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPensionIncomeCYAController @Inject()(implicit val mcc: MessagesControllerComponents,
                                             auditProvider: AuditActionsProvider,
                                             view: UkPensionIncomeCYAView,
                                             appConfig: AppConfig,
                                             pensionSessionService: PensionSessionService,
                                             employmentPensionService: EmploymentPensionService,
                                             errorHandler: ErrorHandler,
                                             clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int): Action[AnyContent] = auditProvider.ukPensionIncomeViewAuditing(taxYear) async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) {
      case (Some(cyaData), _) =>
        val checkRedirect = journeyCheck(CheckUkPensionIncomeCYAPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) {
          data => Future.successful(Ok(view(taxYear, data.pensions.incomeFromPensions)))
        }
      case _ => Future.successful(Redirect(UkPensionSchemePaymentsController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.ukPensionIncomeUpdateAuditing(taxYear) async {
    implicit request =>
      pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
        cya.fold(
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        ) { model =>
          val checkRedirect = journeyCheck(CheckUkPensionIncomeCYAPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(model), cyaPageCall(taxYear))(checkRedirect) {
            data =>
              if (sessionDataDifferentThanPriorData(data.pensions, prior)) {
                employmentPensionService.persistUkPensionIncomeViewModel(request.user, taxYear).map {
                  case Left(_) => errorHandler.internalServerError()
                  case Right(_) => Redirect(IncomeFromPensionsSummaryController.show(taxYear))
                }
              } else {
                Future.successful(Redirect(IncomeFromPensionsSummaryController.show(taxYear)))
              }
          }
        }
      }
  }

  private def sessionDataDifferentThanPriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
      case None => true
    }
  }

}
