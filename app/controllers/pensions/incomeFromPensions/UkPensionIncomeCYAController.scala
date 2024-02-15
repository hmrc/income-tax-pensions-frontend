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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromPensions.routes.IncomeFromPensionsSummaryController
import controllers.predicates.auditActions.AuditActionsProvider
import forms.FormUtils
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateSessionModelFromPrior
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentPensionService
import services.redirects.IncomeFromOtherUkPensionsPages.CheckUkPensionIncomeCYAPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.UkPensionIncomeCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPensionIncomeCYAController @Inject() (mcc: MessagesControllerComponents,
                                              auditProvider: AuditActionsProvider,
                                              view: UkPensionIncomeCYAView,
                                              service: EmploymentPensionService,
                                              errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper
    with FormUtils {

  def show(taxYear: Int): Action[AnyContent] = auditProvider.ukPensionIncomeViewAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CheckUkPensionIncomeCYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>
      Future.successful(Ok(view(taxYear, data.pensions.incomeFromPensions)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.ukPensionIncomeUpdateAuditing(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(CheckUkPensionIncomeCYAPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { sessionData =>
      if (sessionDataDifferentThanPriorData(sessionData.pensions, request.pensions)) {
        service.saveAnswers(request.user, TaxYear(taxYear)).map {
          case Left(_)  => errorHandler.internalServerError()
          case Right(_) => Redirect(IncomeFromPensionsSummaryController.show(taxYear))
        }
      } else {
        Future.successful(Redirect(IncomeFromPensionsSummaryController.show(taxYear)))
      }
    }
  }

  private def sessionDataDifferentThanPriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean =
    priorData match {
      case Some(prior) => !cyaData.equals(generateSessionModelFromPrior(prior))
      case None        => true
    }

}
