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

import config.AppConfig
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{AmountForm, FormUtils}
import forms.QOPSReferenceNumberForm.filter
import forms.validation.mappings.MappingUtil.trimmedText
import models.{AuthorisationRequest, User}
import models.mongo.PensionsUserData
import models.pension.charges.PensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import controllers.pensions.routes.PensionsSummaryController
import play.api.data.Forms.single
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromOverseasPensions.TaxableAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxableAmountController @Inject()(val authAction: AuthorisedAction,
                                        val pensionSessionService: PensionSessionService,
                                        val view: TaxableAmountView,
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
            case Some(i) => populateForm(data, taxYear, i)
//            case None => Redirect(PensionsSummaryController.show(taxYear)) // Todo should redirect to another page
            case None => populateForm(data, taxYear, 0)
          }
        case _ => Redirect(PensionsSummaryController.show(taxYear))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async {
    implicit request =>
      Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
  }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }

  private def populateForm(data: PensionsUserData, taxYear: Int, index: Int)(implicit request: AuthorisationRequest[AnyContent]): Result = {
    val amountBeforeTaxOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentAmount
    val nonUkTaxPaidOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid
    val taxableAmountOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).taxableAmount
    val ftcrOpt = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion
    
    Ok(view(amountForm(request.user), amountBeforeTaxOpt, nonUkTaxPaidOpt, taxableAmountOpt, ftcrOpt, taxYear, Some(index)))
  }

  def amountForm(user: User): Form[BigDecimal] = AmountForm.amountForm("")
}
