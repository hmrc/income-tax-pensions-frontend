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

package controllers.pensions.incomeFromOverseasPensions

import common.MessageKeys.IncomeFromOverseasPensions.ForeignTaxCreditRelief
import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.pensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.PensionsUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsPages.ForeignTaxCreditReliefPage
import services.redirects.IncomeFromOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.ForeignTaxCreditReliefView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignTaxCreditReliefController @Inject()(authAction: AuthorisedAction,
                                                 view: ForeignTaxCreditReliefView,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler)
                                                (implicit mcc: MessagesControllerComponents,
                                                 appConfig: AppConfig, clock: Clock,
                                                 ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int, index: Option[Int] = None): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, ForeignTaxCreditReliefPage, taxYear) { data =>

          val form = populateForm(data, index.getOrElse(0))
          Future.successful(Ok(view(form, taxYear, index.getOrElse(0))))

        }
      case Right(None) => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, index: Option[Int] = None): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, ForeignTaxCreditReliefPage, taxYear) { data =>

          form(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index.getOrElse(0)))),
            validForm =>
              onValidForm(data, taxYear, validForm, index.getOrElse(0)))
        }
      case _ =>
        Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }

  private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(missingInputError = ForeignTaxCreditRelief.noEntry.get(isAgent))

  private def populateForm(pensionsUserData: PensionsUserData, index: Int)
                          (implicit request: AuthorisationRequest[AnyContent]): Form[Boolean] = {
    val baseForm = form(request.user.isAgent)
    pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion match {
      case Some(true) => baseForm.fill(true)
      case Some(false) => baseForm.fill(false)
      case None => baseForm
    }
  }

  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: Boolean, index: Int)
                         (implicit request: AuthorisationRequest[AnyContent], clock: Clock): Future[Result] = {

    val incomeFromOverseasPension = pensionsUserData.pensions.incomeFromOverseasPensions
    validForm match {
      case yesWasSelected =>
        pensionSessionService.createOrUpdateSessionData(
          request.user,
          pensionsUserData.pensions.copy(
            incomeFromOverseasPensions = incomeFromOverseasPension.copy(
              overseasIncomePensionSchemes = incomeFromOverseasPension.overseasIncomePensionSchemes.updated(
                index,
                incomeFromOverseasPension.overseasIncomePensionSchemes(index).copy(
                  foreignTaxCreditReliefQuestion = Some(yesWasSelected)
                )
              )
            )),
          taxYear,
          pensionsUserData.isPriorSubmission
        )(errorHandler.handleError(INTERNAL_SERVER_ERROR))(
          Redirect(TaxableAmountController.show(taxYear, Some(index))))
    }
  }

}
