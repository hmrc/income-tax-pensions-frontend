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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.unauthorisedPayments.RemovePSTRView
import controllers.pensions.unauthorisedPayments.routes.{UnauthorisedPaymentsCYAController, UkPensionSchemeDetailsController}
import controllers.pensions.routes.PensionsSummaryController


import javax.inject.Inject
import scala.concurrent.Future

class RemovePSTRController @Inject()(implicit val mcc: MessagesControllerComponents,
                                     authAction: AuthorisedAction,
                                     removePensionSchemeView: RemovePSTRView,
                                     appConfig: AppConfig,
                                     pensionSessionService: PensionSessionService,
                                     errorHandler: ErrorHandler,
                                     clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pstrList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)
        checkIndexScheme(pensionSchemeIndex, pstrList) match {
          case Some(scheme) =>
            Future.successful(Ok(removePensionSchemeView(taxYear, scheme, pensionSchemeIndex)))
          case _ =>
            Future.successful(Redirect(UkPensionSchemeDetailsController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(UnauthorisedPaymentsCYAController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        val pensionsCYAModel = data.pensions
        val viewModel = pensionsCYAModel.unauthorisedPayments
        val pstrList: Seq[String] = viewModel.pensionSchemeTaxReference.getOrElse(Seq.empty)

        checkIndexScheme(pensionSchemeIndex, pstrList) match {
          case Some(_) =>

            val rawPstrList: Seq[String] = pstrList.patch(pensionSchemeIndex.get, Nil, 1)

            val updatedPstrList = if(rawPstrList.isEmpty) None else Some(rawPstrList)

            val updatedCyaModel = pensionsCYAModel.copy(unauthorisedPayments = viewModel.copy(pensionSchemeTaxReference = updatedPstrList))

            //TODO - call API to remove pension scheme
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(UkPensionSchemeDetailsController.show(taxYear))
            }
          case _ =>
            Future.successful(Redirect(UkPensionSchemeDetailsController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(UnauthorisedPaymentsCYAController.show(taxYear)))
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
