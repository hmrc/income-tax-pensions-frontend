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
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoPensionPages.RetirementAnnuityAmountPage
import services.redirects.PaymentsIntoPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.{isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoPensions.RetirementAnnuityAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetirementAnnuityAmountController @Inject() (authAction: AuthorisedAction,
                                                   retirementAnnuityAmountView: RetirementAnnuityAmountView,
                                                   pensionSessionService: PensionSessionService,
                                                   errorHandler: ErrorHandler,
                                                   formProvider: PaymentsIntoPensionFormProvider,
                                                   mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      val checkRedirect = journeyCheck(RetirementAnnuityAmountPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
        val form = formProvider.retirementAnnuityAmountForm
        data.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments match {
          case Some(amount) =>
            Future.successful(Ok(retirementAnnuityAmountView(form.fill(amount), taxYear)))
          case None => Future.successful(Ok(retirementAnnuityAmountView(form, taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider.retirementAnnuityAmountForm
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(retirementAnnuityAmountView(formWithErrors, taxYear))),
        amount =>
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
            val checkRedirect = journeyCheck(RetirementAnnuityAmountPage, _, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val updatedCyaModel: PensionsCYAModel = pensionsCYAModel.copy(
                paymentsIntoPension = pensionsCYAModel.paymentsIntoPension.copy(totalRetirementAnnuityContractPayments = Some(amount)))

              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                errorHandler.internalServerError()) {
                isFinishedCheck(updatedCyaModel.paymentsIntoPension, taxYear, WorkplacePensionController.show(taxYear), cyaPageCall)
              }
            }
          }
      )
  }

}
