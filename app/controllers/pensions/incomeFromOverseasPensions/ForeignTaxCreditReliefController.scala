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

import common.MessageKeys.IncomeFromOverseasPensions.ForeignTaxCreditRelief
import common.MessageKeys.YesNoForm
import config.{AppConfig, ErrorHandler}
import controllers.BaseYesNoWithIndexController
import controllers.predicates.AuthorisedAction
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import routes._
import services.PensionSessionService
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.ForeignTaxCreditReliefView

import javax.inject.Inject


class ForeignTaxCreditReliefController @Inject()(messagesControllerComponents: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 view: ForeignTaxCreditReliefView,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler)
                                                (implicit appConfig: AppConfig, clock: Clock)
  extends BaseYesNoWithIndexController(messagesControllerComponents, pensionSessionService, authAction, errorHandler) with I18nSupport {

  override protected def redirectToSummaryPage(taxYear: Int): Result = Redirect(controllers.pensions.routes.OverseasPensionsSummaryController.show(taxYear))

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int, index:Int)
                          (implicit request: AuthorisationRequest[AnyContent]): Html = view(populateForm(pensionsUserData, index), taxYear, index)

  override def redirectWhenNoSessionData(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def redirectAfterUpdatingSessionData(taxYear: Int, index:Option[Int]): Result = Redirect(TaxableAmountController.show(taxYear, index))

  override def questionOpt(pensionsUserData: PensionsUserData, index:Int): Option[Boolean] =
    pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion

  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, index:Int): PensionsCYAModel = {
    val incomeFromOverseasPension = currentSessionData.pensions.incomeFromOverseasPensions
    currentSessionData.pensions.copy(
      incomeFromOverseasPensions = incomeFromOverseasPension.copy(
        overseasIncomePensionSchemes =
          incomeFromOverseasPension.overseasIncomePensionSchemes.updated(index,
            incomeFromOverseasPension.overseasIncomePensionSchemes(index).copy(foreignTaxCreditReliefQuestion = Some(yesSelected)))
      ))
  }
  override def whenFormIsInvalid(form: Form[Boolean], taxYear: Int, index:Int)
                                (implicit request: AuthorisationRequest[AnyContent]): Html = view(form, taxYear, index)

  override def errorMessageSet: YesNoForm = ForeignTaxCreditRelief

  override def whenSessionDataIsInsufficient(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData): Boolean = true
}
