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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.FormUtils
import models.AuthorisationRequest
import models.mongo.PensionsUserData
import models.pension.charges.PensionScheme
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.pensions.incomeFromOverseasPensions.routes.{CountrySummaryListController, PensionSchemeSummaryController}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromOverseasPensions.TaxableAmountView

import java.text.NumberFormat
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxableAmountController @Inject()(val authAction: AuthorisedAction,
                                        val pensionSessionService: PensionSessionService,
                                        val taxableAmountView: TaxableAmountView,
                                        errorHandler: ErrorHandler
                                                         )(implicit val mcc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           clock: Clock,
                                                           ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionSessionData(taxYear, request.user).map {
        case Right(Some(data)) =>
          validateIndex(index, data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes) match {
            case Some(i) =>
              if (validTaxAmounts(data, i)) {
                val (amountBeforeTax, signedNonUkTaxPaid, taxableAmount, ftcr) = populateView(data, i)
                Ok(taxableAmountView(amountBeforeTax, signedNonUkTaxPaid, taxableAmount, ftcr, taxYear, index))
              } else {
                Redirect(OverseasPensionsSummaryController.show(taxYear))
              }
            case None => Redirect(OverseasPensionsSummaryController.show(taxYear)) // Todo should redirect to another page
          }
        case _ => Redirect(OverseasPensionsSummaryController.show(taxYear))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async {
    implicit request =>
      pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
        case Right(Some(data)) =>
          validateIndex(index, data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes) match {
            case Some(i) =>
              val updatedCyaModel = updatePensionScheme(data, getTaxableAmount(data, i), taxYear, i)
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(PensionSchemeSummaryController.show(taxYear, index))
              }
            case None => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
          }
        case _ =>
          Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
  }
  
   /***************************************  Helper functions  *****************************************/
  
  private def validTaxAmounts(data: PensionsUserData, index: Int): Boolean = {
    val amountBeforeTax = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    
    if (amountBeforeTax.isEmpty || nonUkTaxPaidOpt.isEmpty) false else true
  }

  private def populateView(data: PensionsUserData, index: Int)
                          (implicit request: AuthorisationRequest[AnyContent]): (Option[String], Option[String], Option[String], Option[Boolean]) = {
    val amountBeforeTax = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    val signedNonUkTaxPaid = nonUkTaxPaidOpt.map(v => if (v > 0) -v else v)
    val ftcr = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion
    val taxableAmount = getTaxableAmount(data, index)
    val viewValues = formatValues(amountBeforeTax, signedNonUkTaxPaid, taxableAmount)
    (viewValues._1, viewValues._2, viewValues._3, ftcr)
  }

  private def formatValues(amountBeforeTax: Option[BigDecimal],
                           signedNonUkTaxPaid: Option[BigDecimal],
                           taxableAmount: Option[BigDecimal]): (Option[String], Option[String], Option[String]) = {
    def formatNoZeros(amount: BigDecimal): String = {
      NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
        .replaceAll("\\.00", "")
    }

    (
      amountBeforeTax.map(amount => formatNoZeros(amount)),
      signedNonUkTaxPaid.map(amount => formatNoZeros(amount)),
      taxableAmount.map(amount => formatNoZeros(amount))
    )
  }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }

  private def getTaxableAmount(data: PensionsUserData, index: Int) = {
    val amountBeforeTaxOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    for {
      amountBeforeTax <- amountBeforeTaxOpt
      nonUkTaxPaid <- nonUkTaxPaidOpt
      isFtcr <- data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion
      taxableAmount = if (isFtcr) {
        amountBeforeTax
      } else {
        amountBeforeTax - nonUkTaxPaid
      }
    } yield taxableAmount
  }

  private def updatePensionScheme(data: PensionsUserData, taxableAmount: Option[BigDecimal], taxYear: Int, index: Int) = {
    val viewModel = data.pensions.incomeFromOverseasPensions
    data.pensions.copy(
      incomeFromOverseasPensions = viewModel.copy(
        overseasIncomePensionSchemes = viewModel.overseasIncomePensionSchemes
          .updated(index, viewModel.overseasIncomePensionSchemes(index)
            .copy(taxableAmount = taxableAmount))

      ))
  }
}
