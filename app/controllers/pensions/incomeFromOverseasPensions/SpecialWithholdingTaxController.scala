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

import common.MessageKeys.UnauthorisedPayments.SpecialWithholdingTax
import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.pensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.RadioButtonAmountForm
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsPages.SpecialWithholdingTaxPage
import services.redirects.IncomeFromOverseasPensionsRedirects.{indexCheckThenJourneyCheck, schemeIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.SpecialWithholdingTaxView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SpecialWithholdingTaxController @Inject() (authAction: AuthorisedAction,
                                                 view: SpecialWithholdingTaxView,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 cc: MessagesControllerComponents)(implicit appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, SpecialWithholdingTaxPage, taxYear) { data =>
          val form = populateForm(data, index.getOrElse(0))
          Future.successful(Ok(view(form, taxYear, index)))

        }
      case Right(None) => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, SpecialWithholdingTaxPage, taxYear) { data =>
          form(request.user.isAgent)
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
              validForm => onValidForm(data, taxYear, validForm, index.getOrElse(0)))

        }
      case Right(None) =>
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  private def onValidForm(pensionsUserData: PensionsUserData, taxYear: Int, validForm: (Boolean, Option[BigDecimal]), index: Int)(implicit
      request: AuthorisationRequest[AnyContent],
      clock: Clock): Future[Result] =
    validForm match {
      case (yesWasSelected, amountOpt) =>
        val ifopData: IncomeFromOverseasPensionsViewModel = pensionsUserData.pensions.incomeFromOverseasPensions
        val updatedSchemes: Seq[PensionScheme] = ifopData.overseasIncomePensionSchemes
          .updated(
            index,
            ifopData
              .overseasIncomePensionSchemes(index)
              .copy(specialWithholdingTaxQuestion = Some(yesWasSelected), specialWithholdingTaxAmount = amountOpt))
        val updatedCyaModel: PensionsCYAModel =
          pensionsUserData.pensions.copy(incomeFromOverseasPensions = ifopData.copy(overseasIncomePensionSchemes = updatedSchemes))

        pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionsUserData.isPriorSubmission)(
          errorHandler.handleError(INTERNAL_SERVER_ERROR))(
          schemeIsFinishedCheck(updatedSchemes, index, taxYear, ForeignTaxCreditReliefController.show(taxYear, Some(index))))
    }

  private def populateForm(pensionsUserData: PensionsUserData, index: Int)(implicit
      request: AuthorisationRequest[AnyContent]): Form[(Boolean, Option[BigDecimal])] = {
    val baseForm = form(request.user.isAgent)
    val scheme   = pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index)
    scheme.specialWithholdingTaxQuestion match {
      case Some(true)  => baseForm.fill((true, scheme.specialWithholdingTaxAmount))
      case Some(false) => baseForm.fill((false, None))
      case None        => baseForm
    }
  }

  private def form(isAgent: Boolean): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = SpecialWithholdingTax.neitherYesNorNo.get(isAgent),
    emptyFieldKey = SpecialWithholdingTax.amountEmpty.get(isAgent),
    wrongFormatKey = SpecialWithholdingTax.amountHasInvalidFormat.get(isAgent),
    minAmountKey = SpecialWithholdingTax.minAmountMessage.get(isAgent),
    exceedsMaxAmountKey = SpecialWithholdingTax.amountIsExcessive.get(isAgent)
  )

}
