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

package controllers.pensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes._
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.ReliefAtSourceOneOffPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class ReliefAtSourceOneOffPaymentsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                       appConfig: AppConfig,
                                                       authAction: AuthorisedAction,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       view: ReliefAtSourceOneOffPaymentsView,
                                                       clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief match {
          case Some(totalRASPaymentsAndTaxRelief) =>
            data.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion match {
              case Some(question) => Future.successful(Ok(view(yesNoForm.fill(question), taxYear, totalRASPaymentsAndTaxRelief)))
              case None => Future.successful(Ok(view(yesNoForm, taxYear, totalRASPaymentsAndTaxRelief)))
            }
          case _ => Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
        }
      case _ => Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief match {
          case Some(totalRASPaymentsAndTaxRelief) =>
            yesNoForm.bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, totalRASPaymentsAndTaxRelief))),
              yesNo => {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val paymentsIntoPension = pensionsCYAModel.paymentsIntoPension

                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(paymentsIntoPension = paymentsIntoPension.copy(oneOffRasPaymentPlusTaxReliefQuestion = Some(yesNo),
                    totalOneOffRasPaymentPlusTaxRelief = if (yesNo) paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief else None))
                }
                pensionSessionService.createOrUpdateSessionData(
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  if (yesNo) {
                    Redirect(OneOffRASPaymentsAmountController.show(taxYear))
                  } else {
                    Redirect(PensionsTaxReliefNotClaimedController.show(taxYear))
                  }
                }
              }
            )
          case _ => Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
        }
      case _ => Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.reliefAtSourceOneOffPayments.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

}
