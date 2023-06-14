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
import controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsCYAController
import controllers.predicates.TaxYearAction.taxYearAction
import controllers.predicates.AuthorisedAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.annualAllowances.RemoveAnnualAllowancesPstrView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemovePstrController @Inject()(implicit val mcc: MessagesControllerComponents,
                                     authAction: AuthorisedAction,
                                     removePensionSchemeView: RemoveAnnualAllowancesPstrView,
                                     appConfig: AppConfig,
                                     pensionSessionService: PensionSessionService,
                                     errorHandler: ErrorHandler,
                                     clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pstrList: Seq[String] = data.pensions.pensionsAnnualAllowances.pensionSchemeTaxReferences.getOrElse(Seq.empty)
        checkIndexScheme(pensionSchemeIndex, pstrList) match {
          case Some(scheme) =>
            Future.successful(Ok(removePensionSchemeView(taxYear, scheme, pensionSchemeIndex)))
          case _ =>
            Future.successful(Redirect(PstrSummaryController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PstrSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionsCYAModel = data.pensions
        val pstrList: Seq[String] = pensionsCYAModel.pensionsAnnualAllowances.pensionSchemeTaxReferences.getOrElse(Seq.empty)

        checkIndexScheme(pensionSchemeIndex, pstrList) match {
          case Some(_) =>

            val rawPstrList: Seq[String] = pstrList.patch(pensionSchemeIndex.get, Nil, 1)

            val updatedPstrList = if(rawPstrList.isEmpty) None else Some(rawPstrList)

            val updatedCyaModel = pensionsCYAModel.copy(pensionsAnnualAllowances = pensionsCYAModel.pensionsAnnualAllowances.copy(
              pensionSchemeTaxReferences = updatedPstrList))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(PstrSummaryController.show(taxYear))
            }
        }
      case _ =>
        Future.successful(Redirect(PstrSummaryController.show(taxYear)))
    }
  }

  private def checkIndexScheme(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[String]): Option[String] = {
    pensionSchemeIndex match {
      case Some(index) if pensionSchemesList.size > index =>
        Some(pensionSchemesList(index))
      case _ =>
        None
    }
  }

}
