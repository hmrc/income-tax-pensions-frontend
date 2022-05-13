/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.predicates.AuthorisedAction
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.incomeFromPensions.UkPensionIncomeSummary

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import controllers.pensions.routes.PensionsSummaryController

class UkPensionIncomeSummaryController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 ukPensionIncomeSummary: UkPensionIncomeSummary,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService) extends FrontendController(cc) with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val incomeFromPensionList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes
        Future(Ok(ukPensionIncomeSummary(taxYear, incomeFromPensionList)))
      case None => Future(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }
}
