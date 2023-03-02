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

package controllers.pensions.shortServiceRefunds

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes._
import controllers.pensions.shortServiceRefunds.routes.RefundSummaryController
import controllers.predicates.ActionsProvider
import controllers.validateIndex
import forms.Countries
import forms.overseas.PensionSchemeForm.{TcSsrPensionsSchemeFormModel, tcSsrPensionSchemeForm}
import models.User
import models.mongo.PensionsUserData
import models.pension.charges.OverseasRefundPensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.shortServiceRefunds.ShortServicePensionsSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ShortServicePensionsSchemeController @Inject()(actionsProvider: ActionsProvider,
                                                     pensionSessionService: PensionSessionService,
                                                     view: ShortServicePensionsSchemeView,
                                                     errorHandler: ErrorHandler)
                                                    (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {
    implicit userSessionDataRequest =>
      val ssrPensionSchemes = userSessionDataRequest.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
      validateIndex(index, ssrPensionSchemes.size) match {
        case Some(idx) =>
          val isUKScheme = ssrPensionSchemes(idx).ukRefundCharge.contains(true)
          val form = rfPensionSchemeForm(userSessionDataRequest.user, isUKScheme).fill(updateFormModel(ssrPensionSchemes(idx)))
          Ok(view(form, taxYear, isUKScheme, idx))
        case _ =>
          Redirect(OverseasPensionsSummaryController.show(taxYear))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      val rfPensionSchemes = userSessionDataRequest.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
      validateIndex(index, rfPensionSchemes.size) match {
        case Some(idx) =>
          val isUKScheme = rfPensionSchemes(idx).ukRefundCharge.contains(true)
          rfPensionSchemeForm(userSessionDataRequest.user, isUKScheme).bindFromRequest().fold(
            formWithErrors =>
               Future.successful(BadRequest(view(formWithErrors, taxYear, isUKScheme, idx)))
              ,
            ssrPensionScheme => {
              val updatedCYAModel = updateViewModel(userSessionDataRequest.pensionsUserData, ssrPensionScheme, idx)
              pensionSessionService.createOrUpdateSessionData(userSessionDataRequest.user, updatedCYAModel, taxYear,
                               userSessionDataRequest.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(RefundSummaryController.show(taxYear))
              }
            }
          )
        case _ =>
          Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
  }
   
  
  private def rfPensionSchemeForm(user: User, isUKScheme: Boolean): Form[TcSsrPensionsSchemeFormModel] =
    tcSsrPensionSchemeForm(
      agentOrIndividual = if (user.isAgent) "agent" else "individual", isUKScheme
  )
  
  private def updateFormModel(scheme: OverseasRefundPensionScheme) =
    TcSsrPensionsSchemeFormModel(
      providerName = scheme.name.getOrElse(""),
      schemeReference = (if (scheme.ukRefundCharge.contains(true)) scheme.pensionSchemeTaxReference else scheme.qualifyingRecognisedOverseasPensionScheme)
        .getOrElse(""),
      providerAddress = scheme.providerAddress.getOrElse(""),
      countryId = scheme.alphaTwoCountryCode.fold{
        Countries.get2AlphaCodeFrom3AlphaCode(scheme.alphaThreeCountryCode)
      } {
        alpha2 => Some(alpha2)
      }
    )
  
  private def updateViewModel(pensionsUserdata: PensionsUserData, scheme: TcSsrPensionsSchemeFormModel, index: Int) = {
    val viewModel = pensionsUserdata.pensions.shortServiceRefunds
    val updatedScheme = {
      val commonUpdatedScheme = viewModel.refundPensionScheme(index)
        .copy(name = Some(scheme.providerName), providerAddress = Some(scheme.providerAddress))
      
      if (commonUpdatedScheme.ukRefundCharge.contains(true)) {
        commonUpdatedScheme.copy(pensionSchemeTaxReference = Some(scheme.schemeReference))
      } else {
        commonUpdatedScheme.copy(qualifyingRecognisedOverseasPensionScheme = Some(scheme.schemeReference), alphaTwoCountryCode = scheme.countryId,
          alphaThreeCountryCode = Countries.get3AlphaCodeFrom2AlphaCode(scheme.countryId))
      }
    }
    pensionsUserdata.pensions.copy(
      shortServiceRefunds = viewModel.copy(
         refundPensionScheme= viewModel.refundPensionScheme.updated(index,updatedScheme)
      )
    )
  }

  
}