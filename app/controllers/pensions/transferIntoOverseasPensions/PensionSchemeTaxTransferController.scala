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
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.ActionsProvider
import forms.RadioButtonAmountForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Request}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.pensionSchemeTaxTransferChargeVeiw

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionSchemeTaxTransferController @Inject()(actionsProvider: ActionsProvider, pensionSessionService: PensionSessionService, view: pensionSchemeTaxTransferChargeVeiw, errorHandler: ErrorHandler)
                                                  (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def amountForm(implicit user:User): Form[(Boolean, Option[BigDecimal])] = {
    val agentOrIndividual = if (user.isAgent) "agent" else "individual"
    RadioButtonAmountForm.radioButtonAndAmountForm(
      missingInputError = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noEntry.$agentOrIndividual",
      emptyFieldKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.noAmountEntry.$agentOrIndividual",
      wrongFormatKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.incorrectFormat.$agentOrIndividual",
      exceedsMaxAmountKey = s"transferIntoOverseasPensions.overseasPensionSchemeTaxTransferCharge.error.tooBig.$agentOrIndividual"
    )}


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
          val transferSchemeChargeAmount: Option[BigDecimal] = sessionData.pensionsUserData.pensions.transfersIntoOverseasPensions.pensionSchemeTransferChargeAmount
          val transferSchemeCharge: Option[Boolean] = sessionData.pensionsUserData.pensions.transfersIntoOverseasPensions.pensionSchemeTransferCharge
          (transferSchemeCharge, transferSchemeChargeAmount) match {
            case (Some(a), amount) => Future.successful(Ok(view(amountForm(sessionData.user).fill((a, amount)), taxYear)))
            case _ =>  Future.successful(Ok(view(amountForm(sessionData.user), taxYear)))
          }
   }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
          amountForm(sessionData.user).bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
            yesNoAmount => {
              (yesNoAmount._1, yesNoAmount._2) match {
                case (true, amount) => updateSessionData(sessionData.pensionsUserData, yesNo = true, amount, taxYear)
                case (false, _) => updateSessionData(sessionData.pensionsUserData, yesNo = false, None, taxYear)
              }
            }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal] = None,
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]) = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      transfersIntoOverseasPensions = pensionUserData.pensions.transfersIntoOverseasPensions.copy(
        pensionSchemeTransferCharge = Some(yesNo),
        pensionSchemeTransferChargeAmount = amount))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(controllers.pensions.transferIntoOverseasPensions.routes.PensionSchemeTaxTransferController.show(taxYear))
    }
  }
}