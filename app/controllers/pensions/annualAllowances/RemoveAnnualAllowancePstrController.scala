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
import controllers.pensions.annualAllowances.routes.PstrSummaryController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.AnnualAllowancesPages.RemovePSTRPage
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.annualAllowances.RemoveAnnualAllowancesPstrView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveAnnualAllowancePstrController @Inject() (cc: MessagesControllerComponents,
                                                     authAction: AuthorisedAction,
                                                     view: RemoveAnnualAllowancesPstrView,
                                                     pensionSessionService: PensionSessionService,
                                                     errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
          redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data =>
            val scheme = data.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.get(pensionSchemeIndex.get)
            Future.successful(Ok(view(taxYear, scheme, pensionSchemeIndex)))
          }
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data =>
          val viewModel: PensionAnnualAllowancesViewModel = data.pensions.pensionsAnnualAllowances
          val rawPstrList: Seq[String] =
            viewModel.pensionSchemeTaxReferences.getOrElse(Seq.empty).patch(pensionSchemeIndex.get, Nil, 1)
          val updatedPstrList: Option[Seq[String]] = if (rawPstrList.isEmpty) None else Some(rawPstrList)

          val updatedCyaModel = data.pensions.copy(pensionsAnnualAllowances = viewModel.copy(pensionSchemeTaxReferences = updatedPstrList))

          pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
            errorHandler.internalServerError()) {
            Redirect(PstrSummaryController.show(taxYear))
          }
        }
      case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

}
