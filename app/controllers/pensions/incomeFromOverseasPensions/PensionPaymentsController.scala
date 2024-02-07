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

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.pensions.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.FormsProvider
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsPages.PensionsPaymentsAmountPage
import services.redirects.IncomeFromOverseasPensionsRedirects.{indexCheckThenJourneyCheck, schemeIsFinishedCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.PensionPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionPaymentsController @Inject() (
    authAction: AuthorisedAction,
    view: PensionPaymentsView,
    pensionSessionService: PensionSessionService,
    formsProvider: FormsProvider,
    errorHandler: ErrorHandler)(implicit mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, PensionsPaymentsAmountPage, taxYear) { data =>
          val form = populateForm(data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index.getOrElse(0)))
          Future.successful(Ok(view(form, taxYear, index)))

        }
      case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        indexCheckThenJourneyCheck(data, index, PensionsPaymentsAmountPage, taxYear) { data =>
          formsProvider
            .pensionPaymentsForm(request.user)
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
              amounts => updatePensionScheme(data, amounts._1, amounts._2, taxYear, index.getOrElse(0))
            )
        }
      case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }

  private def populateForm(pensionScheme: PensionScheme)(implicit
      request: AuthorisationRequest[AnyContent]): Form[(Option[BigDecimal], Option[BigDecimal])] =
    (pensionScheme.pensionPaymentAmount, pensionScheme.pensionPaymentTaxPaid) match {
      case (Some(amountBeforeTax), None) =>
        formsProvider
          .pensionPaymentsForm(request.user)
          .fill((Some(amountBeforeTax), None): (Option[BigDecimal], Option[BigDecimal]))
      case (None, Some(nonUkTaxPaid)) =>
        formsProvider
          .pensionPaymentsForm(request.user)
          .fill((None, Some(nonUkTaxPaid)): (Option[BigDecimal], Option[BigDecimal]))
      case (Some(amountBeforeTax), Some(nonUkTaxPaid)) =>
        formsProvider
          .pensionPaymentsForm(request.user)
          .fill((Some(amountBeforeTax), Some(nonUkTaxPaid)))
      case _ => formsProvider.pensionPaymentsForm(request.user)
    }

  private def updatePensionScheme(data: PensionsUserData,
                                  amountBeforeTaxOpt: Option[BigDecimal],
                                  nonUkTaxPaidOpt: Option[BigDecimal],
                                  taxYear: Int,
                                  index: Int)(implicit request: AuthorisationRequest[AnyContent]): Future[Result] = {
    val viewModel = data.pensions.incomeFromOverseasPensions
    val updatedSchemes: Seq[PensionScheme] = viewModel.overseasIncomePensionSchemes
      .updated(
        index,
        viewModel
          .overseasIncomePensionSchemes(index)
          .updatePensionPayment(amountBeforeTaxOpt, nonUkTaxPaidOpt))
    val updatedCyaModel: PensionsCYAModel =
      data.pensions.copy(incomeFromOverseasPensions = viewModel.copy(overseasIncomePensionSchemes = updatedSchemes))

    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
      errorHandler.internalServerError()) {

      schemeIsFinishedCheck(updatedSchemes, index, taxYear, SpecialWithholdingTaxController.show(taxYear, Some(index)))
    }
  }

}
