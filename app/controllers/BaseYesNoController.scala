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

import common.MessageKeys.YesNoForm
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
abstract class BaseYesNoController @Inject() (cc: MessagesControllerComponents,
                                              pensionSessionService: PensionSessionService,
                                              authAction: AuthorisedAction,
                                              errorHandler: ErrorHandler)(implicit appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(cc) {

  def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Html

  def redirectWhenNoSessionData(taxYear: Int): Result

  def redirectAfterUpdatingSessionData(taxYear: Int): Result

  def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean]

  def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean): PensionsCYAModel

  def whenFormIsInvalid(form: Form[Boolean], taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Html

  def errorMessageSet: YesNoForm

  def whenSessionDataIsInsufficient(taxYear: Int): Result

  def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(pensionsUserDataOpt) =>
        pensionsUserDataOpt
          .map(ensureThatSessionDataIsSufficient(_, taxYear)(show))
          .getOrElse(Future.successful(redirectWhenNoSessionData(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(pensionsUserDataOpt) =>
        pensionsUserDataOpt
          .map(ensureThatSessionDataIsSufficient(_, taxYear)(submit))
          .getOrElse(Future.successful(redirectWhenNoSessionData(taxYear)))
    }
  }

  protected def populateForm(pensionsUserData: PensionsUserData)(implicit request: AuthorisationRequest[AnyContent]): Form[Boolean] = {
    val baseForm = form(request.user.isAgent)
    questionOpt(pensionsUserData) match {
      case Some(true)  => baseForm.fill(true)
      case Some(false) => baseForm.fill(false)
      case None        => baseForm
    }
  }

  protected def redirectToSummaryPage(taxYear: Int): Result = Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  private def show(pensionsUserData: PensionsUserData, taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    Future.successful(Ok(prepareView(pensionsUserData, taxYear)))

  private def ensureThatSessionDataIsSufficient(pensionsUserData: PensionsUserData, taxYear: Int)(
      f: (PensionsUserData, Int) => Future[Result]): Future[Result] =
    if (sessionDataIsSufficient(pensionsUserData)) f(pensionsUserData, taxYear) else Future.successful(whenSessionDataIsInsufficient(taxYear))

  private def onError(implicit request: AuthorisationRequest[AnyContent]): Result = errorHandler.handleError(INTERNAL_SERVER_ERROR)

  private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = errorMessageSet.noEntry.get(isAgent)
  )

  private def submit(pensionsUserData: PensionsUserData, taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    form(request.user.isAgent)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(whenFormIsInvalid(formWithErrors, taxYear))),
        validForm => onValidForm(pensionsUserData, taxYear, validForm))

  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: Boolean)(implicit
      request: AuthorisationRequest[AnyContent],
      clock: Clock): Future[Result] =
    validForm match {
      case yesWasSelected =>
        pensionSessionService.createOrUpdateSessionData(
          request.user,
          proposedUpdatedSessionDataModel(pensionsUserData, yesWasSelected),
          taxYear,
          pensionsUserData.isPriorSubmission)(onError)(redirectAfterUpdatingSessionData(taxYear))
    }
}
