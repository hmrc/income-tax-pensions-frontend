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

package controllers.pensions.employmentFinancedRetirementSchemes

import config.AppConfig
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.YesNoForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.employmentFinancedRetirementSchemes.BenefitsFromSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BenefitsFromSchemeController @Inject() (authAction: AuthorisedAction, view: BenefitsFromSchemeView, cc: MessagesControllerComponents)(implicit
    val appConfig: AppConfig,
    ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with SessionHelper {

  private def benefitsFromSchemeForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employerFinancedRetirementScheme.benefitsFromScheme.error.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    Future.successful(Ok(view(taxYear, benefitsFromSchemeForm(request.user.isAgent))))
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    benefitsFromSchemeForm(request.user.isAgent)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors))),
        yesNoAnswer =>
          if (yesNoAnswer) {
            Future(Redirect(PensionsSummaryController.show(taxYear)))
          } else {
            Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      )
  }
}
