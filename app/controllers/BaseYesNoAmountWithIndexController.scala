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

package controllers

import common.MessageKeys.YesNoAmountForm
import forms.RadioButtonAmountForm
import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
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

abstract class BaseYesNoAmountWithIndexController(messagesControllerComponents: MessagesControllerComponents,
                                         pensionSessionService: PensionSessionService,
                                         authAction: AuthorisedAction,
                                         errorHandler: ErrorHandler)
                                        (implicit appConfig: AppConfig, clock: Clock)
  extends FrontendController(messagesControllerComponents) {
  def prepareView(pensionsUserData: PensionsUserData, taxYear: Int, index : Int)(implicit request: AuthorisationRequest[AnyContent]): Html

  def redirectWhenNoSessionData(taxYear: Int): Result

  def redirectAfterUpdatingSessionData(taxYear: Int, index : Int): Result

  def questionOpt(pensionsUserData: PensionsUserData, index : Int): Option[Boolean]

  def amountOpt(pensionsUserData: PensionsUserData, index : Int): Option[BigDecimal]

  def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal], index : Int): PensionsCYAModel

  def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int, index : Int)
                       (implicit request: AuthorisationRequest[AnyContent]): Html

  def errorMessageSet: YesNoAmountForm

  def whenSessionDataIsInsufficient(taxYear: Int): Result

  def sessionDataIsSufficient(pensionsUserData: PensionsUserData, index : Int): Boolean

  def onError(implicit request: AuthorisationRequest[AnyContent]): Result = errorHandler.handleError(INTERNAL_SERVER_ERROR)

  private def form(isAgent: Boolean): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = errorMessageSet.neitherYesNorNo.get(isAgent),
    emptyFieldKey = errorMessageSet.amountEmpty.get(isAgent),
    wrongFormatKey = errorMessageSet.amountHasInvalidFormat.get(isAgent),
    minAmountKey = errorMessageSet.minAmountMessage.get(isAgent),
    exceedsMaxAmountKey = errorMessageSet.amountIsExcessive.get(isAgent)
  )

  def validateIndex(index: Option[Int], pensionsUserData: PensionsUserData): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.length)
  }

  private def shows(pensionsUserData: PensionsUserData, taxYear: Int, index : Int)
                           (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    Future.successful(Ok(prepareView(pensionsUserData, taxYear, index)))


  private def ensureThatSessionDataIsSufficient(pensionsUserData: PensionsUserData, taxYear: Int, index : Int)
                                               (f: (PensionsUserData, Int, Int) => Future[Result]): Future[Result] =
    if (sessionDataIsSufficient(pensionsUserData, index)) f(pensionsUserData, taxYear, index)else Future.successful(whenSessionDataIsInsufficient(taxYear))


  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(Some(data)) => validateIndex(index, data) match {
        case Some(id) => ensureThatSessionDataIsSufficient(data, taxYear, id)(shows)
        case None => Future.successful(redirectWhenNoSessionData(taxYear))
      }
      case Right(None) => Future.successful(redirectWhenNoSessionData(taxYear))
    }
  }

  private def submit(pensionsUserData: PensionsUserData, taxYear: Int, index : Int)
                             (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    form(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(whenFormIsInvalid(formWithErrors, taxYear, index))),
      validForm => onValidForm(pensionsUserData, taxYear, validForm, index))

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request => {
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(Some(data)) => validateIndex(index, data) match {
        case Some(id) => ensureThatSessionDataIsSufficient(data, taxYear, id)(submit)
        case None => Future.successful(redirectWhenNoSessionData(taxYear))
      }
      case Right(None) => Future.successful(redirectWhenNoSessionData(taxYear))
    }}}

  protected def populateForm(pensionsUserData: PensionsUserData, index : Int)
                            (implicit request: AuthorisationRequest[AnyContent]): Form[(Boolean, Option[BigDecimal])] =
  {
    val baseForm = form(request.user.isAgent)
    questionOpt(pensionsUserData, index) match {
      case Some(true) => baseForm.fill((true, amountOpt(pensionsUserData, index)))
      case Some(false) => baseForm.fill((false, None))
      case None => baseForm
    }
  }


  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: (Boolean, Option[BigDecimal]), index : Int)
                         (implicit request: AuthorisationRequest[AnyContent], clock: Clock): Future[Result] = {
    validForm match {
      case (yesWasSelected, amountOpt) =>
        pensionSessionService.createOrUpdateSessionData(
          request.user,
          proposedUpdatedSessionDataModel(pensionsUserData, yesWasSelected, amountOpt, index),
          taxYear,
          pensionsUserData.isPriorSubmission)(onError)(redirectAfterUpdatingSessionData(taxYear, index))
    }
  }

  protected def redirectToSummaryPage(taxYear: Int): Result = Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))


}
