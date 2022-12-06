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

package controllers.pensions.overseas.incomeFromOverseasPension

import common.MessageKeys
import common.MessageKeys.UnauthorisedPayments.SpecialWithholdingTax
import config.{AppConfig, ErrorHandler}
import controllers.{BaseYesNoAmountController, BaseYesNoAmountWithIndexController}
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.AuthorisationRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PensionSessionService
import utils.Clock
import views.html.pensions.overseas.incomeFromOverseasPension.SpecialWithholdingTaxView

class SpecialWithholdingTaxController @Inject()(messagesControllerComponents: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                view: SpecialWithholdingTaxView,
                                                pensionSessionService: PensionSessionService,
                                                errorHandler: ErrorHandler)
                                               (implicit appConfig: AppConfig, clock: Clock)
  extends BaseYesNoAmountWithIndexController(messagesControllerComponents, pensionSessionService, authAction, errorHandler) with I18nSupport{

  // TODO: Should we be redirecting to the CYA Page? It doesn't quite make sense as we won't have any session data.
  override def redirectWhenNoSessionData(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def redirectAfterUpdatingSessionData(taxYear: Int, index : Option[Int] = None): Result =
    Redirect(controllers.pensions.overseas.incomeFromOverseasPension.routes.SpecialWithholdingTaxController.show(taxYear, index))


  override def questionOpt(pensionsUserData: PensionsUserData, index : Option[Int]): Option[Boolean] = {

    validateIndex(index, pensionsUserData) match {
      case Some(id) => pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(id).specialWithholdingTaxQuestion
      case None => None
    }
  }

  override def amountOpt(pensionsUserData: PensionsUserData, index : Option[Int]): Option[BigDecimal] =
      validateIndex(index, pensionsUserData) match {
        case Some(id) => {
          pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(id).specialWithholdingTaxAmount
        }
        case None => None
      }


  override def proposedUpdatedSessionDataModel(currentSessionData: PensionsUserData, yesSelected: Boolean, amountOpt: Option[BigDecimal], index : Option[Int]): PensionsCYAModel = {
    {
      index match {
        case Some(index) => {
          currentSessionData.pensions.copy(
            incomeFromOverseasPensions = currentSessionData.pensions.incomeFromOverseasPensions.copy(
              overseasIncomePensionSchemes = currentSessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
                .updated(index, currentSessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index).copy(
                  specialWithholdingTaxQuestion = Some(yesSelected),
                  specialWithholdingTaxAmount = amountOpt
                ))
            )
          )
        }
        case None => currentSessionData.pensions
      }
    }
  }



  override def whenFormIsInvalid(form: Form[(Boolean, Option[BigDecimal])], taxYear: Int, index : Option[Int])
                                (implicit request: AuthorisationRequest[AnyContent]): Html = {
    view(form, taxYear, index)
  }

  override def errorMessageSet: MessageKeys.YesNoAmountForm = SpecialWithholdingTax

  override def whenSessionDataIsInsufficient(taxYear: Int): Result = redirectToSummaryPage(taxYear)

  override def sessionDataIsSufficient(pensionsUserData: PensionsUserData, index : Option[Int]): Boolean =
    validateIndex(index, pensionsUserData) match {
      case Some(id) => {
        pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(id).pensionPaymentAmount.isDefined ||
          pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(id).pensionPaymentTaxPaid.isDefined
      }
      case None => false
    }

  override def prepareView(pensionsUserData: PensionsUserData, taxYear: Int, index : Option[Int])
                          (implicit request: AuthorisationRequest[AnyContent]): Html = view(populateForm(pensionsUserData, index), taxYear, index)
}
