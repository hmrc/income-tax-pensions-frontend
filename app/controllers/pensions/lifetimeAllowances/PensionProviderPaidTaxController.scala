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

package controllers.pensions.lifetimeAllowances

import common.MessageKeys.LifetimeAllowance.PensionProviderPaidTax
import common.MessageKeys.YesNoAmountForm
import config.{AppConfig, ErrorHandler}
import controllers.BaseYesNoAmountController
import controllers.predicates.AuthorisedAction
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.PensionSessionService
import utils.Clock
import views.html.pensions.lifetimeAllowances.PensionProviderPaidTaxView

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PensionProviderPaidTaxController @Inject()(messagesControllerComponents: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 pensionProviderPaidTaxView: PensionProviderPaidTaxView,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler)
                                                (implicit appConfig: AppConfig, clock: Clock)
  extends BaseYesNoAmountController(messagesControllerComponents, pensionSessionService, authAction, errorHandler) with I18nSupport {

  override val errorMessageSet: YesNoAmountForm = PensionProviderPaidTax

  // TODO: Once we've creating the CYA page (in SASS-2470), we can redirect to it.
  override def redirectWhenNoSessionData(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def redirectAfterUpdatingSessionData(pensionsUserData: PensionsUserData, taxYear: Int): Result =
    redirectToNext(pensionsUserData, taxYear)

  private def redirectToNext(pensionsUserData: PensionsUserData, taxYear: Int) = {
    val pstrs = pensionsUserData.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences
    if(pstrs.exists(_.nonEmpty)) {
      Redirect(controllers.pensions.annualAllowances.routes.PstrSummaryController.show(taxYear: Int))
    }
    else {
      Redirect(controllers.pensions.lifetimeAllowances.routes.PensionSchemeTaxReferenceLifetimeController.show(taxYear, None))
    }

  }

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)
                          (implicit request: AuthorisationRequest[AnyContent]): Html = pensionProviderPaidTaxView(populateForm(pensionsUserData), taxYear)

  override def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int)
                                (implicit request: AuthorisationRequest[AnyContent]): Html = pensionProviderPaidTaxView(form, taxYear)

  override def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean] =
    pensionsUserData.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion

  override def amountOpt(pensionsUserData: PensionsUserData): Option[BigDecimal] = pensionsUserData.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider

  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal]): PensionsCYAModel =
    currentSessionData.pensions.copy(
      pensionsAnnualAllowances = currentSessionData.pensions.pensionsAnnualAllowances.copy(
        pensionProvidePaidAnnualAllowanceQuestion = Some(yesSelected),
        taxPaidByPensionProvider = amountOpt.filter(_ => yesSelected)
      )
    )

  override def whenSessionDataIsInsufficient(pensionsUserData: PensionsUserData, taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean = true

}
