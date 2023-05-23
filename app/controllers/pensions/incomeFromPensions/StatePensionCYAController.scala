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

import config.AppConfig
import controllers.pensions.incomeFromPensions.routes.IncomeFromPensionsSummaryController
import controllers.predicates.{ActionsProvider, AuthorisedAction}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.StatePensionCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StatePensionCYAController @Inject()(authAction: AuthorisedAction,
                                          actionsProvider: ActionsProvider,
                                          pensionSessionService: PensionSessionService,
                                          view: StatePensionCYAView)
                                         (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {implicit userSessionDataRequest =>
     Ok(view(taxYear, userSessionDataRequest.pensionsUserData.pensions.incomeFromPensions))
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        if (comparePriorData(model.pensions, prior)) {
          //TODO - build submission model from cya data and submit to DES if cya data doesn't match prior data
          Future.successful(Redirect(IncomeFromPensionsSummaryController.show(taxYear)))
        } else {
          Future.successful(Redirect(IncomeFromPensionsSummaryController.show(taxYear)))
        }
      }
    }
  }

  private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
      case None => true
    }
  }

}
