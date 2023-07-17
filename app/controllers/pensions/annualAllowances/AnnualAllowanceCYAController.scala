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

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.{ActionsProvider, AuthorisedAction}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowances.AnnualAllowancesCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AnnualAllowanceCYAController @Inject()(authAction: AuthorisedAction,
                                             actionsProvider: ActionsProvider,
                                             view: AnnualAllowancesCYAView,
                                             pensionSessionService: PensionSessionService,
                                             errorHandler: ErrorHandler)
                                            (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), _) =>
            Future.successful(Ok(view(taxYear, data.pensions.pensionsAnnualAllowances)))
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user,
            cyaModel, taxYear, isPriorSubmission = false)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.pensionsAnnualAllowances))
          )
        case (None, None) =>
          val emptyAnnualAllowanceModel: PensionAnnualAllowancesViewModel = PensionAnnualAllowancesViewModel()
          Future.successful(Ok(view(taxYear, emptyAnnualAllowanceModel )))
        case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        if (sessionDataDifferentThanPriorData(model.pensions, prior)) {
          //        TODO - build submission model from cya data and submit to DES if cya data doesn't match prior data (SASS-3444)
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        } else {
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
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
