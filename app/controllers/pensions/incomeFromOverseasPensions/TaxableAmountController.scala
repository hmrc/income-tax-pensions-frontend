/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{AuthorisationRequest, User}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.incomeFromOverseasPensions.routes.TaxableAmountController
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromOverseasPensions.TaxableAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxableAmountController @Inject()(val authAction: AuthorisedAction,
                                        val pensionSessionService: PensionSessionService,
                                        val view: TaxableAmountView,
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
              if (validTaxAmounts(data, taxYear, i)) {
                populateView(data, taxYear, i)
              } else {
                Redirect(PensionsSummaryController.show(taxYear))
              }
            case None => Redirect(PensionsSummaryController.show(taxYear)) // Todo should redirect to another page
          }
        case _ => Redirect(PensionsSummaryController.show(taxYear))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async {
    implicit request =>
      pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
        case Right(Some(data)) =>
          validateIndex(index, data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes) match {
            case Some(i) => updatePensionScheme(data, getTaxableAmount(data, i), taxYear, i)
            case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        case _ =>
          //TODO: redirect to the lifetime allowance CYA page?
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }

  private def validTaxAmounts(data: PensionsUserData, taxYear: Int, index: Int): Boolean = {
    val amountBeforeTax = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    if (amountBeforeTax == None || nonUkTaxPaidOpt == None) {
      false
    } else {
      true
    }
  }

  private def populateView(data: PensionsUserData, taxYear: Int, index: Int)(implicit request: AuthorisationRequest[AnyContent]): Result = {
    val amountBeforeTax = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    val signedNonUkTaxPaid = nonUkTaxPaidOpt.map(v => if (v > 0) -v else v)
    val ftcr = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion
    val taxableAmount = getTaxableAmount(data, index)

    Ok(view(amountBeforeTax, signedNonUkTaxPaid, taxableAmount, ftcr, taxYear, Some(index)))
  }

  private def getTaxableAmount(data: PensionsUserData, index: Int) = {
    val amountBeforeTaxOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    for {
      amountBeforeTax <- amountBeforeTaxOpt
      nonUkTaxPaid <- nonUkTaxPaidOpt
      isFtcr <- data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion
      taxableAmount = if(isFtcr) {
        amountBeforeTax - nonUkTaxPaid
      } else {
        amountBeforeTax
      }
    } yield taxableAmount
  }

  private def updatePensionScheme(data: PensionsUserData, taxableAmount: Option[BigDecimal], taxYear: Int, index: Int)
                                 (implicit request: AuthorisationRequest[AnyContent]) = {
    val viewModel = data.pensions.incomeFromOverseasPensions
    val updatedCyaModel: PensionsCYAModel = {
      data.pensions.copy(
        incomeFromOverseasPensions = viewModel.copy(
          overseasIncomePensionSchemes = viewModel.overseasIncomePensionSchemes
            .updated(index, viewModel.overseasIncomePensionSchemes(index)
              .copy(taxableAmount = taxableAmount))

        ))
    }
    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
      //TODO: Redirect to next page when navigation is available
      Redirect(TaxableAmountController.show(taxYear, Some(index)))
    }
  }
}
