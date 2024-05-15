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

import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.pensions.transferIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.{OverseasTransferChargeAmountPage, TaxOnPensionSchemesAmountPage}
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.pensionSchemeTaxTransferChargeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionSchemeTaxTransferController @Inject() (actionsProvider: ActionsProvider,
                                                    pensionSessionService: PensionSessionService,
                                                    view: pensionSchemeTaxTransferChargeView,
                                                    formsProvider: FormsProvider,
                                                    errorHandler: ErrorHandler,
                                                    mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(TaxOnPensionSchemesAmountPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val transferSchemeChargeAmount: Option[BigDecimal] =
        data.pensions.transfersIntoOverseasPensions.pensionSchemeTransferChargeAmount
      val transferSchemeCharge: Option[Boolean] = data.pensions.transfersIntoOverseasPensions.pensionSchemeTransferCharge
      (transferSchemeCharge, transferSchemeChargeAmount) match {
        case (Some(a), amount) => Future.successful(Ok(view(formsProvider.pensionSchemeTaxTransferForm(request.user).fill((a, amount)), taxYear)))
        case _                 => Future.successful(Ok(view(formsProvider.pensionSchemeTaxTransferForm(request.user), taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    formsProvider
      .pensionSchemeTaxTransferForm(request.user)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNoAmount => {
          val checkRedirect = journeyCheck(OverseasTransferChargeAmountPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
            (yesNoAmount._1, yesNoAmount._2) match {
              case (true, amount) => updateSessionData(data, yesNo = true, amount, taxYear)
              case (false, _)     => updateSessionData(data, yesNo = false, None, taxYear)
            }
          }
        }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, yesNo: Boolean, amount: Option[BigDecimal], taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {
    val cyaModel  = pensionUserData.pensions
    val viewModel = cyaModel.transfersIntoOverseasPensions
    val updatedModel: PensionsCYAModel = cyaModel.copy(transfersIntoOverseasPensions = viewModel.copy(
      pensionSchemeTransferCharge = Some(yesNo),
      pensionSchemeTransferChargeAmount = amount,
      transferPensionScheme = if (yesNo) viewModel.transferPensionScheme else Seq.empty
    ))

    pensionSessionService.createOrUpdateSessionData(request.user, updatedModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(
        if (!yesNo || updatedModel.transfersIntoOverseasPensions.isFinished) {
          TransferIntoOverseasPensionsCYAController.show(TaxYear(taxYear))
        } else {
          redirectForSchemeLoop(schemes = updatedModel.transfersIntoOverseasPensions.transferPensionScheme, taxYear)
        }
      )
    }
  }
}
