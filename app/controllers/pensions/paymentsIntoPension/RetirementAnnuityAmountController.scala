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
import controllers.pensions.paymentsIntoPension.routes._
import controllers.predicates.AuthorisedAction
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import utils.PaymentsIntoPensionPages.RetirementAnnuityAmountPage
import views.html.pensions.paymentsIntoPensions.RetirementAnnuityAmountView
import javax.inject.{Inject, Singleton}
import models.redirects.ConditionalRedirect
import services.RedirectService.{isFinishedCheck, PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future


@Singleton
class RetirementAnnuityAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  retirementAnnuityAmountView: RetirementAnnuityAmountView,
                                                  appConfig: AppConfig,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "pensions.retirementAnnuityAmount.error.noEntry",
    wrongFormatKey = "pensions.retirementAnnuityAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "pensions.retirementAnnuityAmount.error.overMaximum"
  )


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        data.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments match {
          case Some(amount) =>
            Future.successful(Ok(retirementAnnuityAmountView(amountForm.fill(amount), taxYear)))
          case None => Future.successful(Ok(retirementAnnuityAmountView(amountForm, taxYear)))
        }
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(retirementAnnuityAmountView(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(totalRetirementAnnuityContractPayments = Some(amount)))
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel, taxYear, WorkplacePensionController.show(taxYear))
            }}
        }
      }
    )
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(RetirementAnnuityAmountPage, cya, taxYear) ++
      Seq(ConditionalRedirect(
        cya.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion.contains(false),
        controllers.pensions.paymentsIntoPension.routes.RetirementAnnuityController.show(taxYear)
      ))
  }

}
