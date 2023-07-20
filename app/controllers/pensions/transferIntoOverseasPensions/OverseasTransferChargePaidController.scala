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
import controllers._
import controllers.pensions.transferIntoOverseasPensions.routes.TransferPensionsSchemeController
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.pages.OverseasTransferChargePaidPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{OverseasTransferChargesService, PensionSessionService}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.{DidAUKPensionSchemePayTransferChargePage, TaxOnPensionSchemesAmountPage}
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.OverseasTransferChargesPaidView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasTransferChargePaidController @Inject()(actionsProvider: ActionsProvider,
                                                     formsProvider: FormsProvider,
                                                     pageView: OverseasTransferChargesPaidView,
                                                     pensionSessionService: PensionSessionService,
                                                     errorHandler: ErrorHandler,
                                                     overseasTransferChargesService: OverseasTransferChargesService
                                                    )(implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionUserData =>
      val updatedUserData = cleanUpSchemes(sessionUserData.pensionsUserData)
      val checkRedirect = journeyCheck(DidAUKPensionSchemePayTransferChargePage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>

    validatedSchemes(pensionSchemeIndex, data.pensions.transfersIntoOverseasPensions.transferPensionScheme) match {
      case Left(_) => Future.successful(Redirect(redirectForSchemeLoop(data.pensions.transfersIntoOverseasPensions.transferPensionScheme, taxYear)))
      case Right(_) => Future.successful(Ok(
        pageView(OverseasTransferChargePaidPage(
          taxYear, pensionSchemeIndex, data.pensions.transfersIntoOverseasPensions, formsProvider.overseasTransferChargePaidForm))))
          }
      }
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData): PensionsUserData = {
    val schemes = pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme
    val filteredSchemes = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel = pensionsUserData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(transfersIntoOverseasPensions = updatedViewModel)
    val updatedUserData = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData)
    updatedUserData
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit sessionUserData =>

      val checkRedirect = journeyCheck(DidAUKPensionSchemePayTransferChargePage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionUserData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        data =>

      val schemes = data.pensions.transfersIntoOverseasPensions.transferPensionScheme
      validatedSchemes(pensionSchemeIndex, schemes) match {
        case Left(_) => Future.successful(Redirect(redirectForSchemeLoop(schemes, taxYear)))
        case Right(_) => formsProvider.overseasTransferChargePaidForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(pageView(OverseasTransferChargePaidPage(
                taxYear, pensionSchemeIndex, data.pensions.transfersIntoOverseasPensions, formWithErrors)))),
          yesNoValue => {
            overseasTransferChargesService.updateOverseasTransferChargeQuestion(data, yesNoValue, pensionSchemeIndex).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(userData) => Redirect(TransferPensionsSchemeController.show(
                taxYear, Some(pensionSchemeIndex.getOrElse(userData.pensions.transfersIntoOverseasPensions.transferPensionScheme.size - 1))))
            }
          }
        )
      }
    }
  }
}
}

