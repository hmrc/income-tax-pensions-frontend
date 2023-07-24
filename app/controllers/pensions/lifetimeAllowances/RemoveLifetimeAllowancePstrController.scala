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

package controllers.pensions.lifetimeAllowances

import config.{AppConfig, ErrorHandler}
import controllers.pensions.lifetimeAllowances.routes.LifetimePstrSummaryController
import controllers.predicates.actions.ActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionLifetimeAllowancesViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.LifetimeAllowancesPages.RemovePSTRPage
import services.redirects.LifetimeAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.lifetimeAllowances.RemoveLifetimeAllowancesPstrView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveLifetimeAllowancePstrController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      actionsProvider: ActionsProvider,
                                                      removePSTRView: RemoveLifetimeAllowancesPstrView,
                                                      appConfig: AppConfig,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request =>
      val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
      redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>
          val scheme = data.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences.get(pensionSchemeIndex.get)
          Future.successful(Ok(removePSTRView(taxYear, scheme, pensionSchemeIndex)))
      }
  }


  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request =>
      val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
      redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>
          val viewModel: PensionLifetimeAllowancesViewModel = data.pensions.pensionLifetimeAllowances
          val rawPstrList: Seq[String] =
            viewModel.pensionSchemeTaxReferences.getOrElse(Seq.empty).patch(pensionSchemeIndex.get, Nil, 1)
          val updatedPstrList: Option[Seq[String]] = if (rawPstrList.isEmpty) None else Some(rawPstrList)

          val updatedCyaModel = data.pensions.copy(pensionLifetimeAllowances = viewModel.copy(
            pensionSchemeTaxReferences = updatedPstrList))

          pensionSessionService.createOrUpdateSessionData(request.user,
            updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
            Redirect(LifetimePstrSummaryController.show(taxYear))
          }
      }
  }
}
