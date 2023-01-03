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

package controllers.pensions.paymentsIntoOverseasPensions

import common.MessageKeys.OverseasPensions.PaymentIntoScheme
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
import views.html.pensions.paymentsIntoOverseasPensions.PaymentIntoPensionSchemeView

import javax.inject.Inject

class PaymentIntoPensionSchemeController @Inject()(messagesControllerComponents: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   paymentIntoPensionSchemeView: PaymentIntoPensionSchemeView,
                                                   pensionSessionService: PensionSessionService,
                                                   errorHandler: ErrorHandler)
                                                  (implicit appConfig: AppConfig, clock: Clock)
  extends BaseYesNoAmountController(messagesControllerComponents, pensionSessionService, authAction, errorHandler) with I18nSupport {

  override val errorMessageSet: YesNoAmountForm = PaymentIntoScheme

  // TODO: Once we've creating the CYA page (in SASS-3268), we can redirect to it.
  override def redirectWhenNoSessionData(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def redirectAfterUpdatingSessionData(taxYear: Int): Result =
    Redirect(controllers.pensions.paymentsIntoOverseasPensions.routes.PaymentIntoPensionSchemeController.show(taxYear))

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)
                          (implicit request: AuthorisationRequest[AnyContent]): Html = paymentIntoPensionSchemeView(populateForm(pensionsUserData), taxYear)

  override def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int)
                                (implicit request: AuthorisationRequest[AnyContent]): Html = paymentIntoPensionSchemeView(form, taxYear)

  override def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean] =
    pensionsUserData.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions

  override def amountOpt(pensionsUserData: PensionsUserData): Option[BigDecimal] =
    pensionsUserData.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount

  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal]): PensionsCYAModel =
    currentSessionData.pensions.copy(
      paymentsIntoOverseasPensions = currentSessionData.pensions.paymentsIntoOverseasPensions.copy(
        paymentsIntoOverseasPensionsQuestions = Some(yesSelected),
        paymentsIntoOverseasPensionsAmount = amountOpt.filter(_ => yesSelected)
      )
    )

  override def whenSessionDataIsInsufficient(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean = true

}
