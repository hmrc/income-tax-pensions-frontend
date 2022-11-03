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

package controllers

import common.MessageKeys.YesNoAmountForm
import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.RadioButtonAmountForm
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


abstract class BaseYesNoAmountController(
                                          messagesControllerComponents: MessagesControllerComponents,
                                          pensionSessionService: PensionSessionService,
                                          authAction: AuthorisedAction,
                                          errorHandler: ErrorHandler)
                                        (implicit appConfig: AppConfig, clock: Clock)
  extends FrontendController(messagesControllerComponents) {

  def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)
                 (implicit request: AuthorisationRequest[AnyContent]): Html

  def redirectWhenNoSessionData(taxYear: Int): Result

  def redirectAfterUpdatingSessionData(taxYear: Int): Result

  def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean]

  def amountOpt(pensionsUserData: PensionsUserData): Option[BigDecimal]

  def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal]): PensionsCYAModel

  def onInvalidForm(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int)
                   (implicit request: AuthorisationRequest[AnyContent]): Html

  def errorMessageSet: YesNoAmountForm

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).map {
      case Left(_) => onError
      case Right(pensionsUserDataOpt) =>
        pensionsUserDataOpt
          .map(pensionUserData => Ok(prepareView(pensionUserData, taxYear)))
          .getOrElse(redirectWhenNoSessionData(taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async {
    implicit request => {
      pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
        case Left(_) => Future.successful(onError)
        case Right(pensionsUserDataOpt) =>
          pensionsUserDataOpt
            .map(submit(_, taxYear))
            .getOrElse(Future.successful(redirectWhenNoSessionData(taxYear)))
      }
    }
  }

  protected def populateForm(pensionsUserData: PensionsUserData)
                            (implicit request: AuthorisationRequest[AnyContent]): Form[(Boolean, Option[BigDecimal])] = {
    val baseForm = form(request.user.isAgent)
    questionOpt(pensionsUserData) match {
      case Some(true) => baseForm.fill((true, amountOpt(pensionsUserData)))
      case Some(false) => baseForm.fill((false, None))
      case None => baseForm
    }
  }

  protected def redirectToSummaryPage(taxYear: Int): Result = Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  private def onError(implicit request: AuthorisationRequest[AnyContent]): Result = errorHandler.handleError(INTERNAL_SERVER_ERROR)

  private def form(isAgent: Boolean): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = errorMessageSet.neitherYesNorNo.get(isAgent),
    emptyFieldKey = errorMessageSet.amountEmpty.get(isAgent),
    wrongFormatKey = errorMessageSet.amountHasInvalidFormat.get(isAgent),
    exceedsMaxAmountKey = errorMessageSet.amountIsExcessive.get(isAgent)
  )

  private def submit(pensionsUserData: PensionsUserData, taxYear: Int)
                    (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    form(request.user.isAgent).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(onInvalidForm(formWithErrors, taxYear))),
      validForm => onValidForm(pensionsUserData, taxYear, validForm))

  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: (Boolean, Option[BigDecimal]))
                         (implicit request: AuthorisationRequest[AnyContent], clock: Clock): Future[Result] =
    validForm match {
      case (yesWasSelected, amountOpt) =>
        pensionSessionService.createOrUpdateSessionData(
          request.user,
          proposedUpdatedSessionDataModel(pensionsUserData, yesWasSelected, amountOpt),
          taxYear,
          pensionsUserData.isPriorSubmission)(onError)(redirectAfterUpdatingSessionData(taxYear))
    }

}
