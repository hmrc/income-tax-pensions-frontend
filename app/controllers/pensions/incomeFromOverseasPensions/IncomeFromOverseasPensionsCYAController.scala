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
import controllers.pensions.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import javax.inject.Inject
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import routes._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsCYAView

import scala.concurrent.Future

class IncomeFromOverseasPensionsCYAController @Inject()(authAction: AuthorisedAction,
                                                        view: IncomeFromOverseasPensionsCYAView,
                                                        pensionSessionService: PensionSessionService,
                                                        errorHandler: ErrorHandler)
                                                       (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), _) => Future.successful(Ok(view(taxYear, data.pensions.incomeFromOverseasPensions)))
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user,
            cyaModel, taxYear, isPriorSubmission = false)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.incomeFromOverseasPensions))
          )
        case (None, None) =>
          val emptyIncomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion= None,
          overseasIncomePensionSchemes = Nil)
          Future.successful(Ok(view(taxYear, emptyIncomeFromOverseasPensionsViewModel)))
        case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, _) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { _ =>
        //TODO - build submission model from cya data and submit to DES if cya data doesn't match prior data
        Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
    }
  }
}
