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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PensionIncomeService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeFromOverseasPensionsCYAController @Inject()(authAction: AuthorisedAction,
                                                        view: IncomeFromOverseasPensionsCYAView,
                                                        pensionSessionService: PensionSessionService,
                                                        pensionIncomeService: PensionIncomeService,
                                                        errorHandler: ErrorHandler)
                                                       (implicit val mcc: MessagesControllerComponents,
                                                        appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>

    def cyaDataIsEmpty(priorData: AllPensionsData): Future[Result] = {
      val cyaModel = generateCyaFromPrior(priorData)
      pensionSessionService.createOrUpdateSessionData(request.user,
        cyaModel, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(
        Ok(view(taxYear, cyaModel.incomeFromOverseasPensions)))
    }

    def incomeFromOverseasPensionsViewModelExists(cya: IncomeFromOverseasPensionsViewModel): Future[Result] = {
      Future.successful(Ok(view(taxYear, cya)))
    }

    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), Some(priorData: AllPensionsData)) if data.pensions.incomeFromOverseasPensions.isEmpty() =>
          cyaDataIsEmpty(priorData)
        case (Some(data), _) =>
          incomeFromOverseasPensionsViewModelExists(data.pensions.incomeFromOverseasPensions)
        case (None, Some(priorData)) =>
          cyaDataIsEmpty(priorData)
        case (None, None) =>
          val emptyIncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion = None,
            overseasIncomePensionSchemes = Nil)
          Future.successful(Ok(view(taxYear, emptyIncomeFromOverseasPensionsViewModel)))
        case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    val p1 = pensionIncomeService.saveIncomeFromOverseasPensionsViewModel(request.user, taxYear)
      p1.map {
      case Left(_) =>
        errorHandler.internalServerError()
      case Right(_) => Redirect(controllers.pensions.routes.OverseasPensionsSummaryController.show(taxYear))
    }
  }
}
