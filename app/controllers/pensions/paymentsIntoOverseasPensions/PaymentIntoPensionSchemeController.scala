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
import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.BaseYesNoAmountController
import controllers.pensions.paymentsIntoOverseasPensions.routes.{EmployerPayOverseasPensionController, PaymentsIntoOverseasPensionsCYAController}
import controllers.predicates.actions.AuthorisedAction
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import views.html.pensions.paymentsIntoOverseasPensions.PaymentIntoPensionSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PaymentIntoPensionSchemeController @Inject() (cc: MessagesControllerComponents,
                                                    authAction: AuthorisedAction,
                                                    view: PaymentIntoPensionSchemeView,
                                                    pensionSessionService: PensionSessionService,
                                                    errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BaseYesNoAmountController(cc, pensionSessionService, authAction, errorHandler)
    with I18nSupport {

  override val errorMessageSet: YesNoAmountForm = PaymentIntoScheme

  override def redirectWhenNoSessionData(taxYear: Int): Result = Redirect(controllers.pensions.routes.OverseasPensionsSummaryController.show(taxYear))

  override def redirectAfterUpdatingSessionData(pensionsUserData: PensionsUserData, taxYear: Int): Result = {
    val model = pensionsUserData.pensions.paymentsIntoOverseasPensions
    Redirect(
      if (model.paymentsIntoOverseasPensionsQuestions.getOrElse(false)) {
        EmployerPayOverseasPensionController.show(taxYear)
      } else {
        PaymentsIntoOverseasPensionsCYAController.show(TaxYear(taxYear))
      }
    )
  }

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Html =
    view(populateForm(cleanUpReliefs(pensionsUserData)), taxYear)

  override def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int)(implicit request: AuthorisationRequest[AnyContent]): Html =
    view(form, taxYear)

  override def questionOpt(pensionsUserData: PensionsUserData): Option[Boolean] =
    pensionsUserData.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions

  override def amountOpt(pensionsUserData: PensionsUserData): Option[BigDecimal] =
    pensionsUserData.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount

  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData,
                                               yesSelected: Boolean,
                                               amountOpt: Option[BigDecimal]): PensionsCYAModel = {
    val piopData: PaymentsIntoOverseasPensionsViewModel =
      if (yesSelected) {
        currentSessionData.pensions.paymentsIntoOverseasPensions
          .copy(paymentsIntoOverseasPensionsQuestions = Some(true), paymentsIntoOverseasPensionsAmount = amountOpt)
      } else {
        PaymentsIntoOverseasPensionsViewModel(paymentsIntoOverseasPensionsQuestions = Some(false))
      }
    currentSessionData.pensions.copy(paymentsIntoOverseasPensions = piopData)
  }

  override def whenSessionDataIsInsufficient(pensionsUserData: PensionsUserData, taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean = true

  private def cleanUpReliefs(pensionsUserData: PensionsUserData): PensionsUserData = {
    val reliefs            = pensionsUserData.pensions.paymentsIntoOverseasPensions.schemes
    val filteredReliefs    = if (reliefs.nonEmpty) reliefs.filter(relief => relief.isFinished) else reliefs
    val updatedViewModel   = pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(schemes = filteredReliefs)
    val updatedPensionData = pensionsUserData.pensions.copy(paymentsIntoOverseasPensions = updatedViewModel)
    val updatedUserData    = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSession(updatedUserData)
    updatedUserData
  }
}
