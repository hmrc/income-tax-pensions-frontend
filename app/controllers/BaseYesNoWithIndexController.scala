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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


abstract class BaseYesNoWithIndexController(
                                             messagesControllerComponents: MessagesControllerComponents,
                                             pensionSessionService: PensionSessionService,
                                             authAction: AuthorisedAction,
                                             errorHandler: ErrorHandler)
                                           (implicit appConfig: AppConfig, clock: Clock)
  extends FrontendController(messagesControllerComponents) {

  def prepareView(pensionsUserData: PensionsUserData, taxYear: Int, index: Int)
                 (implicit request: AuthorisationRequest[AnyContent]): Html

  def redirectWhenNoSessionData(taxYear: Int): Result

  def redirectAfterUpdatingSessionData(taxYear: Int, index: Option[Int]): Result

  def questionOpt(pensionsUserData: PensionsUserData, index: Int): Option[Boolean]

  def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, index: Int): PensionsCYAModel

  def whenFormIsInvalid(form: Form[Boolean], taxYear: Int, index: Int)
                       (implicit request: AuthorisationRequest[AnyContent]): Html

  def errorMessageSet: YesNoForm

  def whenSessionDataIsInsufficient(taxYear: Int): Result

  def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean

  def show(taxYear: Int, index: Option[Int] = None): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(pensionsUserDataOpt) =>
        pensionsUserDataOpt
          .map(pensions => {
            validateIndex(index, pensions.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes).fold(
              Future.successful(redirectWhenNoSessionData(taxYear))
            )(i => {
              ensureThatSessionDataIsSufficient(pensions, taxYear, i)(show)
            })
          }).getOrElse(Future.successful(redirectWhenNoSessionData(taxYear)))
    }
  }

  def submit(taxYear: Int, index: Option[Int] = None): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(onError)
      case Right(pensionsUserDataOpt) =>
        pensionsUserDataOpt
          .map(pensions => {
            validateIndex(index, pensions.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes).fold(
              Future.successful(redirectWhenNoSessionData(taxYear))
            )(i => {
              ensureThatSessionDataIsSufficient(pensions, taxYear, i)(submit)
            })
          }).getOrElse(Future.successful(redirectWhenNoSessionData(taxYear)))
    }
  }

  protected def populateForm(pensionsUserData: PensionsUserData, index: Int)
                            (implicit request: AuthorisationRequest[AnyContent]): Form[Boolean] = {
    val baseForm = form(request.user.isAgent)
    questionOpt(pensionsUserData, index) match {
      case Some(true) => baseForm.fill(true)
      case Some(false) => baseForm.fill(false)
      case None => baseForm
    }
  }

  protected def redirectToSummaryPage(taxYear: Int): Result = Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  private def show(pensionsUserData: PensionsUserData, taxYear: Int, index: Int)
                  (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    Future.successful(Ok(prepareView(pensionsUserData, taxYear, index)))


  private def ensureThatSessionDataIsSufficient(pensionsUserData: PensionsUserData, taxYear: Int, index: Int)(f: (PensionsUserData, Int, Int) => Future[Result])
                                               (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    if (sessionDataIsSufficient(pensionsUserData)) f(pensionsUserData, taxYear, index) else Future.successful(whenSessionDataIsInsufficient(taxYear))


  private def onError(implicit request: AuthorisationRequest[AnyContent]): Result = errorHandler.handleError(INTERNAL_SERVER_ERROR)

  private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = errorMessageSet.noEntry.get(isAgent)
  )


  private def submit(pensionsUserData: PensionsUserData, taxYear: Int, index: Int)
                    (implicit request: AuthorisationRequest[AnyContent]): Future[Result] =
    form(request.user.isAgent).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(whenFormIsInvalid(formWithErrors, taxYear, index))),
      validForm => onValidForm(pensionsUserData, taxYear, validForm, index))

  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: Boolean, index: Int)
                         (implicit request: AuthorisationRequest[AnyContent], clock: Clock): Future[Result] =
    validForm match {
      case (yesWasSelected) =>
        pensionSessionService.createOrUpdateSessionData(
          request.user,
          proposedUpdatedSessionDataModel(pensionsUserData, yesWasSelected, index),
          taxYear,
          pensionsUserData.isPriorSubmission)(onError)(redirectAfterUpdatingSessionData(taxYear, Some(index)))
    }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }
}
