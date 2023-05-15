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

package controllers.pensions.paymentsIntoPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoPensions.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoPensionsRedirects
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import services.redirects.PaymentsIntoPensionPages.RasAmountPage
import utils.Clock
import views.html.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ReliefAtSourcePaymentsAndTaxReliefAmountController @Inject()(authAction: AuthorisedAction,
                                                                   pensionSessionService: PensionSessionService,
                                                                   errorHandler: ErrorHandler,
                                                                   view: ReliefAtSourcePaymentsAndTaxReliefAmountView,
                                                                   formProvider: PaymentsIntoPensionFormProvider)
                                                                  (implicit val mcc: MessagesControllerComponents,
                                                                   appConfig: AppConfig, clock: Clock,
                                                                   ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {
  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = PaymentsIntoPensionsRedirects.journeyCheck(RasAmountPage, _, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData)(checkRedirect) { data =>
          data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief match {
            case Some(amount) => Future.successful(Ok(view(formProvider.reliefAtSourcePaymentsAndTaxReliefAmountForm.fill(amount), taxYear)))
            case None => Future.successful(Ok(view(formProvider.reliefAtSourcePaymentsAndTaxReliefAmountForm, taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider.reliefAtSourcePaymentsAndTaxReliefAmountForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          val checkRedirect = PaymentsIntoPensionsRedirects.journeyCheck(RasAmountPage, _, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, optData)(checkRedirect) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(
                totalRASPaymentsAndTaxRelief = Some(amount), totalPaymentsIntoRASQuestion = None
              ))
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(ReliefAtSourceOneOffPaymentsController.show(taxYear))
            }
          }
        }

      }
    )
  }

}
