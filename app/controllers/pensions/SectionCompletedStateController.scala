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

package controllers.pensions

import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.JourneyStatus
import models.mongo.JourneyStatus.{Completed, InProgress, NotStarted}
import models.pension.Journey
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.FutureUtils.FutureOps
import views.html.pensions.SectionCompletedStateView

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.Future

@Singleton
class SectionCompletedStateController @Inject() (actionsProvider: ActionsProvider,
                                                 formsProvider: FormsProvider,
                                                 cc: MessagesControllerComponents,
                                                 view: SectionCompletedStateView)(implicit appConfig: AppConfig)
    extends FrontendController(cc)
    with I18nSupport { // TODO 7969 add Spec for show and submit

  val form: Form[Boolean] = formsProvider.sectionCompletedStateForm

  def show(taxYear: Int, journey: Journey): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) { implicit request =>
    val existingAnswer: Option[JourneyStatus] = None // TODO 7969 GET journey state
    val filledForm                            = existingAnswer.fold(form)(fill(form, _))
    Ok(view(filledForm, taxYear, journey))
  }

  def submit(taxYear: Int, journey: Journey): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, journey))),
        answer => saveAndRedirect(answer, journey, taxYear)
      )
  }

  private def fill(form: Form[Boolean], status: JourneyStatus): Form[Boolean] =
    status match {
      case Completed  => form.fill(true)
      case InProgress => form.fill(false)
      case NotStarted => form
    }

  private def saveAndRedirect(answer: Boolean, journey: Journey, taxYear: Int): Future[Result] = { // TODO 7969 POST journey state
    @unused
    val status = if (answer) Completed else InProgress
    journey.sectionCompletedRedirect(taxYear).toFuture
  }

}
