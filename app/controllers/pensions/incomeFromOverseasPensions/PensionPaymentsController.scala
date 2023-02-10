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
import controllers.pensions.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{FormUtils, OptionalTupleAmountForm}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import models.{AuthorisationRequest, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import routes._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromOverseasPensions.PensionPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionPaymentsController @Inject()(authAction: AuthorisedAction,
                                          pensionPaymentsView: PensionPaymentsView,
                                          pensionSessionService: PensionSessionService,
                                          errorHandler: ErrorHandler)
                                         (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm(user: User): Form[(Option[BigDecimal], Option[BigDecimal])] = OptionalTupleAmountForm.amountForm(
    emptyFieldKey1 = "overseasPension.pensionPayments.amountBeforeTax.noEntry",
    wrongFormatKey1 = s"overseasPension.pensionPayments.amountBeforeTax.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey1 = "overseasPension.pensionPayments.amountBeforeTax.tooBig",
    emptyFieldKey2 = "common.pensions.error.amount.noEntry",
    wrongFormatKey2 = "overseasPension.pensionPayments.nonUkTaxPaid.incorrectFormat",
    exceedsMaxAmountKey2 = "common.pensions.error.amount.overMaximum"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).map {
        case Right(Some(data)) =>
        validateIndex(index, data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes) match {
          case Some(i) => populateForm(data, taxYear, i)
          case None =>
            Redirect(OverseasPensionsSummaryController.show(taxYear)) // Todo should redirect to another page
        }
      case _ =>
        Redirect(OverseasPensionsSummaryController.show(taxYear))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Right(Some(data)) =>
        validateIndex(index, data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes) match {
          case Some(i) => amountForm(request.user).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(pensionPaymentsView(formWithErrors, taxYear, Some(i)))),
            amounts =>
              updatePensionScheme(data, amounts._1, amounts._2, taxYear, i)(
                Redirect(SpecialWithholdingTaxController.show(taxYear, index))
              )
          )
          case None => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
        }
      case _ =>
        Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }


  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }

  private def populateForm(data: PensionsUserData, taxYear: Int, index: Int)(implicit request: AuthorisationRequest[AnyContent]): Result = {
    val amountBeforeTaxOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid

    val form = (amountBeforeTaxOpt, nonUkTaxPaidOpt) match {
      case (Some(amountBeforeTax), None) => amountForm(request.user).fill(Some(amountBeforeTax), None)
      case (None, Some(nonUkTaxPaid)) => amountForm(request.user).fill((None, Some(nonUkTaxPaid)))
      case (Some(amountBeforeTax), Some(nonUkTaxPaid)) => amountForm(request.user).fill((Some(amountBeforeTax), Some(nonUkTaxPaid)))
      case (_, _) => amountForm(request.user)
    }
    Ok(pensionPaymentsView(form, taxYear, Some(index)))
  }

  private def updatePensionScheme(data: PensionsUserData, amountBeforeTaxOpt: Option[BigDecimal], nonUkTaxPaidOpt: Option[BigDecimal], taxYear: Int, index: Int)
                                 (redirect: Result)
                                 (implicit request: AuthorisationRequest[AnyContent]) = {
    val viewModel = data.pensions.incomeFromOverseasPensions
    val updatedCyaModel: PensionsCYAModel = {
      data.pensions.copy(
        incomeFromOverseasPensions = viewModel.copy(
          overseasIncomePensionSchemes = viewModel.overseasIncomePensionSchemes
            .updated(index, viewModel.overseasIncomePensionSchemes(index)
              .copy(pensionPaymentAmount = amountBeforeTaxOpt, pensionPaymentTaxPaid = nonUkTaxPaidOpt))

        ))
    }
    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
      redirect
    }
  }
}
