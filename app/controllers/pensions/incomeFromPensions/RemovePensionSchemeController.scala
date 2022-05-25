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
import controllers.pensions.incomeFromPensions.routes.UkPensionIncomeSummaryController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import javax.inject.Inject
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.incomeFromPensions.RemovePensionSchemeView

import scala.concurrent.Future

class RemovePensionSchemeController @Inject()(implicit val mcc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              removePensionSchemeView: RemovePensionSchemeView,
                                              appConfig: AppConfig,
                                              pensionSessionService: PensionSessionService) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionIncomesList: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.uKPensionIncomes

        checkIndex(pensionSchemeIndex, pensionIncomesList) match {
          case Some(scheme) =>
            Future.successful(Ok(removePensionSchemeView(taxYear, scheme.pensionSchemeName.getOrElse(""), pensionSchemeIndex)))
          case _ =>
            Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear)))
        }
      case _ =>
        //TODO redirect to Income from Pensions CYA page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  private def checkIndex(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[UkPensionIncomeViewModel]): Option[UkPensionIncomeViewModel] = {
    pensionSchemeIndex match {
      case Some(index) if pensionSchemesList.size > index =>
        Some(pensionSchemesList(index))
      case _ =>
        println(s"\n\nGOT HERE, list:\n$pensionSchemesList\n\n")
        None
    }
  }

}
