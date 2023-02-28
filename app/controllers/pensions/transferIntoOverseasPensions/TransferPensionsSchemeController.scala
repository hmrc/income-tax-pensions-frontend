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

package controllers.pensions.transferIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes._
import controllers.pensions.transferIntoOverseasPensions.routes._
import controllers.predicates.ActionsProvider
import controllers.validateIndex
import forms.Countries
import forms.overseas.PensionSchemeForm.{TcSsrPensionsSchemeFormModel, tcSsrPensionSchemeForm}
import models.User
import models.mongo.PensionsUserData
import models.pension.charges.TransferPensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.TransferPensionsSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TransferPensionsSchemeController @Inject()(actionsProvider: ActionsProvider,
                                                 pensionSessionService: PensionSessionService,
                                                 view: TransferPensionsSchemeView,
                                                 errorHandler: ErrorHandler)
                                                (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) {
    implicit userSessionDataRequest =>
      val tcPensionSchemes = userSessionDataRequest.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme
      validateIndex(index, tcPensionSchemes.size) match {
        case Some(idx) =>
          val isUKScheme = tcPensionSchemes(idx).ukTransferCharge.contains(true)
          val form = tcPensionSchemeForm(userSessionDataRequest.user, isUKScheme).fill(updateFormModel(tcPensionSchemes(idx)))
          Ok(view(form, taxYear, isUKScheme, idx))
        case _ =>
          Redirect(OverseasPensionsSummaryController.show(taxYear))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit userSessionDataRequest =>
      val tcPensionSchemes = userSessionDataRequest.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme
      validateIndex(index, tcPensionSchemes.size) match {
        case Some(idx) =>
          val isUKScheme = tcPensionSchemes(idx).ukTransferCharge.contains(true)
          tcPensionSchemeForm(userSessionDataRequest.user, isUKScheme).bindFromRequest().fold(
            formWithErrors =>
               Future.successful(BadRequest(view(formWithErrors, taxYear, isUKScheme, idx)))
              ,
            tcPensionScheme => {
              val updatedCYAModel = updateViewModel(userSessionDataRequest.pensionsUserData, tcPensionScheme, idx)
              pensionSessionService.createOrUpdateSessionData(userSessionDataRequest.user, updatedCYAModel, taxYear,
                               userSessionDataRequest.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(TransferChargeSummaryController.show(taxYear)) //TODO: redirect to Overseas Transfer charge Pensions list
              }
            }
          )
        case _ =>
          Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
  }
   
  
  private def tcPensionSchemeForm(user: User, isUKScheme: Boolean): Form[TcSsrPensionsSchemeFormModel] =
    tcSsrPensionSchemeForm(
      agentOrIndividual = if (user.isAgent) "agent" else "individual", isUKScheme
  )
  
  private def updateFormModel(scheme: TransferPensionScheme) =
    TcSsrPensionsSchemeFormModel(
      providerName = scheme.name.getOrElse(""),
      schemeReference = (if (scheme.ukTransferCharge.contains(true)) scheme.pstr else scheme.qops)
        .getOrElse(""),
      providerAddress = scheme.providerAddress.getOrElse(""),
      countryId = scheme.alphaTwoCountryCode.fold{
        Countries.get2AlphaCodeFrom3AlphaCode(scheme.alphaThreeCountryCode)
      } {
        alpha2 => Some(alpha2)
      }
    )
  
  private def updateViewModel(pensionsUserdata: PensionsUserData, scheme: TcSsrPensionsSchemeFormModel, index: Int) = {
    val viewModel = pensionsUserdata.pensions.transfersIntoOverseasPensions
    val updatedScheme = {
      val commonUpdatedScheme = viewModel.transferPensionScheme(index)
        .copy(name = Some(scheme.providerName), providerAddress = Some(scheme.providerAddress))
      
      if (commonUpdatedScheme.ukTransferCharge.contains(true)) {
        commonUpdatedScheme.copy(pstr = Some(scheme.schemeReference))
      } else {
        commonUpdatedScheme.copy(qops = Some(scheme.schemeReference), alphaThreeCountryCode = Countries.get3AlphaCodeFrom2AlphaCode(scheme.countryId))
      }
    }
    pensionsUserdata.pensions.copy(
      transfersIntoOverseasPensions = viewModel.copy(
        transferPensionScheme = viewModel.transferPensionScheme.updated(index,updatedScheme)
      )
    )
  }

  
}