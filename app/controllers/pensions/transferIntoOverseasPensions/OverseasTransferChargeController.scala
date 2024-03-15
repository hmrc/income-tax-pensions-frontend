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
import controllers.pensions.transferIntoOverseasPensions.routes.{PensionSchemeTaxTransferController, TransferIntoOverseasPensionsCYAController}
import controllers.predicates.actions.ActionsProvider
import forms.RadioButtonAmountForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.OverseasTransferChargeAmountPage
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.OverseasTransferChargeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class OverseasTransferChargeController @Inject() (actionsProvider: ActionsProvider,
                                                  pensionSessionService: PensionSessionService,
                                                  view: OverseasTransferChargeView,
                                                  errorHandler: ErrorHandler,
                                                  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(OverseasTransferChargeAmountPage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val transferChargeAmount: Option[BigDecimal] = data.pensions.transfersIntoOverseasPensions.overseasTransferChargeAmount
      val transferCharge: Option[Boolean]          = data.pensions.transfersIntoOverseasPensions.overseasTransferCharge
      (transferCharge, transferChargeAmount) match {
        case (Some(a), amount) => Future.successful(Ok(view(amountForm(request.user).fill((a, amount)), taxYear)))
        case _                 => Future.successful(Ok(view(amountForm(request.user), taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    amountForm(request.user)
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

  def amountForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = "transferIntoOverseasPensions.overseasTransferCharge.error.noEntry",
      emptyFieldKey = s"transferIntoOverseasPensions.overseasTransferCharge.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"transferIntoOverseasPensions.overseasTransferCharge.error.incorrectFormat.$agentOrIndividual",
      minAmountKey = "common.error.amountNotZero",
      exceedsMaxAmountKey = s"transferIntoOverseasPensions.overseasTransferCharge.error.tooBig.$agentOrIndividual"
    )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, yesNo: Boolean, amount: Option[BigDecimal], taxYear: Int)(implicit
      request: UserSessionDataRequest[T]): Future[Result] = {

    val cyaModel = pensionUserData.pensions
    val updateViewModel = cyaModel.copy(transfersIntoOverseasPensions = if (yesNo) {
      cyaModel.transfersIntoOverseasPensions.copy(overseasTransferCharge = Some(true), overseasTransferChargeAmount = amount)
    } else {
      TransfersIntoOverseasPensionsViewModel(transferPensionSavings = Some(true), overseasTransferCharge = Some(false))
    })

    pensionSessionService.createOrUpdateSessionData(request.user, updateViewModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(
        if (!yesNo || updateViewModel.transfersIntoOverseasPensions.isFinished) {
          TransferIntoOverseasPensionsCYAController.show(taxYear)
        } else {
          PensionSchemeTaxTransferController.show(taxYear)
        }
      )
    }
  }
}
