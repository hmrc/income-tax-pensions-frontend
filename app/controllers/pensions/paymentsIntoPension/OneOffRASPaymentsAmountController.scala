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

package controllers.pensions.paymentsIntoPension

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.pensions.paymentsIntoPension.routes._
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.RedirectService.{PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.OneOffRasAmountPage
import views.html.pensions.paymentsIntoPensions.OneOffRASPaymentsAmountView

import javax.inject.{Inject, Singleton}
import models.redirects.ConditionalRedirect

import scala.concurrent.Future

@Singleton
class OneOffRASPaymentsAmountController @Inject()(authAction: AuthorisedAction,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: OneOffRASPaymentsAmountView,
                                                  formProvider: PaymentsIntoPensionFormProvider)
                                                 (implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int, fromGatewayChangeLink: Boolean = false): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear, fromGatewayChangeLink)) { data =>

        val viewModel = data.pensions.paymentsIntoPension

        (viewModel.oneOffRasPaymentPlusTaxReliefQuestion,
          viewModel.totalOneOffRasPaymentPlusTaxRelief,
          viewModel.totalRASPaymentsAndTaxRelief
        ) match {
          case (Some(true), amount, Some(rasAmount)) =>
            val form = amount.fold(formProvider.oneOffRASPaymentsAmountForm)(a => formProvider.oneOffRASPaymentsAmountForm.fill(a))
            Future.successful(Ok(view(form, taxYear, rasAmount, fromGatewayChangeLink)))
          case _ => errorHandler.futureInternalServerError()
        }
      }
    }
  }

  def submit(taxYear: Int, fromGatewayChangeLink: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      optData =>
        redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear, fromGatewayChangeLink)) { data =>
          formProvider.oneOffRASPaymentsAmountForm.bindFromRequest.fold(
            formWithErrors => {
              data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief.fold(
                Future.successful(Redirect(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear, fromGatewayChangeLink)))
              )(rasAmount => Future.successful(BadRequest(view(formWithErrors, taxYear, rasAmount))))
            },
            amount => {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(
                  totalOneOffRasPaymentPlusTaxRelief = Some(amount), totalPaymentsIntoRASQuestion = None
                ))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(TotalPaymentsIntoRASController.show(taxYear, fromGatewayChangeLink))
              }
            }
          )
        }
    }
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int, fromGatewayChangeLink: Boolean): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(OneOffRasAmountPage, cya, taxYear) ++
      Seq(ConditionalRedirect(
        cya.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion.contains(false),
        ReliefAtSourceOneOffPaymentsController.show(taxYear, fromGatewayChangeLink)
      ))
  }

}
