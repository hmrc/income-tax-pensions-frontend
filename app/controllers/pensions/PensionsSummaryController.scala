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

package controllers.pensions

import config.AppConfig
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.pension.AllPensionsData
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.PensionsSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class PensionsSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                           appConfig: AppConfig,
                                           authAction: AuthorisedAction,
                                           pensionSessionService: PensionSessionService,
                                           pensionsSummaryView: PensionsSummaryView) extends FrontendController(mcc) with I18nSupport {
  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (pensionsUserData, prior) =>
      val cya = prior.fold(pensionsUserData.map(_.pensions)){pr =>
        if(pensionsUserData.exists(! _.pensions.isEmpty)) pensionsUserData.map(_.pensions) else Some(AllPensionsData.generateCyaFromPrior(pr))}
      Future.successful(Ok(pensionsSummaryView(taxYear, cya, prior)))
    }
  }
}
