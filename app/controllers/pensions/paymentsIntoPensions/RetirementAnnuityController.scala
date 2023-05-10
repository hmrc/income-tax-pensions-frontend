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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.SimpleRedirectService.{PaymentsIntoPensionsRedirects, isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.{RasAmountPage, RetirementAnnuityPage}
import views.html.pensions.paymentsIntoPensions.PayIntoRetirementAnnuityContractView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RetirementAnnuityController @Inject()(authAction: AuthorisedAction,
                                            payIntoRetirementAnnuityContractView: PayIntoRetirementAnnuityContractView,
                                            pensionSessionService: PensionSessionService,
                                            errorHandler: ErrorHandler,
                                            formProvider: PaymentsIntoPensionFormProvider)
                                           (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {
  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      val checkRedirect = PaymentsIntoPensionsRedirects.journeyCheck(RetirementAnnuityPage, _, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, optData)(checkRedirect) { data =>

        val form = formProvider.retirementAnnuityForm(request.user.isAgent)
        data.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion match {
          case Some(value) => Future.successful(Ok(payIntoRetirementAnnuityContractView(
            form.fill(value), taxYear)))
          case None => Future.successful(Ok(payIntoRetirementAnnuityContractView(form, taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider.retirementAnnuityForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(payIntoRetirementAnnuityContractView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          val checkRedirect = PaymentsIntoPensionsRedirects.journeyCheck(RetirementAnnuityPage, _, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, optData)(checkRedirect) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(retirementAnnuityContractPaymentsQuestion = Some(yesNo),
                totalRetirementAnnuityContractPayments = if (yesNo) viewModel.totalRetirementAnnuityContractPayments else None))
            }
            val redirectLocation = if (yesNo) {
              controllers.pensions.paymentsIntoPensions.routes.RetirementAnnuityAmountController.show(taxYear)
            } else {
              controllers.pensions.paymentsIntoPensions.routes.WorkplacePensionController.show(taxYear)
            }

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
            }
          }
        }
      }
    )
  }

//  private def redirects(cya: PensionsCYAModel, taxYear: Int): Either[Result, Unit] = {
//    PaymentsIntoPensionsRedirects.journeyCheck(RasAmountPage, cya, taxYear)
//  }
}
