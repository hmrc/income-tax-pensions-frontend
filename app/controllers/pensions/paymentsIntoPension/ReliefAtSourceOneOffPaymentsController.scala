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

package controllers.pensions.paymentsIntoPension

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoPension.routes._
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.OneOffRasPage
import views.html.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsView

import javax.inject.{Inject, Singleton}
import models.redirects.ConditionalRedirect
import services.RedirectService.{PaymentsIntoPensionsRedirects, isFinishedCheck, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future


@Singleton
class ReliefAtSourceOneOffPaymentsController @Inject()(authAction: AuthorisedAction,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler,
                                                       view: ReliefAtSourceOneOffPaymentsView,
                                                       formProvider: PaymentsIntoPensionFormProvider)
                                                      (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief.fold(Future.successful(errorHandler.internalServerError()))(
          totalRASPaymentsAndTaxRelief =>
            data.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion match {
              case Some(question) =>
                Future.successful(
                  Ok(view(formProvider.reliefAtSourceOneOffPaymentsForm(request.user.isAgent).fill(question), taxYear, totalRASPaymentsAndTaxRelief)))
              case None =>
                Future.successful(Ok(view(formProvider.reliefAtSourceOneOffPaymentsForm(request.user.isAgent), taxYear, totalRASPaymentsAndTaxRelief)))
            }
        )
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        data.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief match {
          case Some(totalRASPaymentsAndTaxRelief) =>
            formProvider.reliefAtSourceOneOffPaymentsForm(request.user.isAgent).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, totalRASPaymentsAndTaxRelief))),
              yesNo => {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val paymentsIntoPension = pensionsCYAModel.paymentsIntoPension

                val updatedCyaModel: PensionsCYAModel = {
                  pensionsCYAModel.copy(paymentsIntoPension = paymentsIntoPension.copy(oneOffRasPaymentPlusTaxReliefQuestion = Some(yesNo),
                    totalOneOffRasPaymentPlusTaxRelief = if (yesNo) paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief else None))
                }
                val redirectLocation = if (yesNo) {
                  OneOffRASPaymentsAmountController.show(taxYear)
                } else {
                  TotalPaymentsIntoRASController.show(taxYear)
                }

                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                    isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
                    Redirect(redirectLocation)
                }
              }
            )
          case _ => Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
        }
      }
    }
  }


  private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(OneOffRasPage, cya, taxYear)
  }

}
