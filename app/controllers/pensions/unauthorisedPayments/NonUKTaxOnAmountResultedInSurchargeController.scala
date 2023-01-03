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

package controllers.pensions.unauthorisedPayments

import common.MessageKeys.UnauthorisedPayments.NonUKTaxOnAmountResultedInSurcharge
import common.MessageKeys.YesNoAmountForm
import config.{AppConfig, ErrorHandler}
import controllers.BaseYesNoAmountController
import controllers.predicates.AuthorisedAction
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import utils.Clock
import views.html.pensions.unauthorisedPayments.NonUKTaxOnAmountResultedInSurchargeView

import javax.inject.Inject

class NonUKTaxOnAmountResultedInSurchargeController @Inject()(messagesControllerComponents: MessagesControllerComponents,
                                                              authAction: AuthorisedAction,
                                                              view: NonUKTaxOnAmountResultedInSurchargeView,
                                                              pensionSessionService: PensionSessionService,
                                                              errorHandler: ErrorHandler)
                                                             (implicit appConfig: AppConfig, clock: Clock)
  extends BaseYesNoAmountController(messagesControllerComponents, pensionSessionService, authAction, errorHandler) with I18nSupport {

  override val errorMessageSet: YesNoAmountForm = NonUKTaxOnAmountResultedInSurcharge

  // TODO: Should we be redirecting to the CYA Page? It doesn't quite make sense as we won't have any session data.
  override def redirectWhenNoSessionData(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  // TODO: We should be redirected to NonUkTaxOnAmountNotSurchargeController if we've answered 'Yes' to unauthorisedPayments.noSurchargeQuestion. See SASS-3228.
  override def redirectAfterUpdatingSessionData(taxYear: Int): Result =
    Redirect(controllers.pensions.unauthorisedPayments.routes.WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear))

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)
                          (implicit request: AuthorisationRequest[AnyContent]): Html = view(populateForm(pensionsUserData), taxYear)

  override def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int)
                                (implicit request: AuthorisationRequest[AnyContent]): Html = view(form, taxYear)

  override def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean] =
    pensionsUserData.pensions.unauthorisedPayments.surchargeTaxAmountQuestion

  override def amountOpt(pensionsUserData: PensionsUserData): Option[BigDecimal] =
    pensionsUserData.pensions.unauthorisedPayments.surchargeTaxAmount

  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal]): PensionsCYAModel =
    currentSessionData.pensions.copy(
      unauthorisedPayments = currentSessionData.pensions.unauthorisedPayments.copy(
        surchargeTaxAmountQuestion = Some(yesSelected),
        surchargeTaxAmount = amountOpt.filter(_ => yesSelected)
      )
    )

  override def whenSessionDataIsInsufficient(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean =
    pensionsUserData.pensions.unauthorisedPayments.surchargeAmount.isDefined

}
